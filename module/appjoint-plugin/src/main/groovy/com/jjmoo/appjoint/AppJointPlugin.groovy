package com.jjmoo.appjoint

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.utils.FileUtils
import com.google.common.collect.Sets
import groovy.io.FileType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.CRC32
import java.util.zip.CheckedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AppJointPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.android.registerTransform(new AppJointTransform(project))
    }

    class AppJointTransform extends Transform {
        private static final String TAG = "AppJointTransform: "
        private static final int BUFFER_SIZE = 50_000
        private Project mProject

        private ClassInfo mAppLikeInfo
        private ClassInfo mModulesInfo
        private ClassInfo mAppInfo
        private def mModuleSpecs = []
        private def mModulesServices = [:]

        AppJointTransform(Project project) {
            mProject = project
        }

        @Override
        String getName() {
            return "appjoint-plugin"
        }

        @Override
        Set<QualifiedContent.ContentType> getInputTypes() {
            return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES)
        }

        @Override
        Set<? super QualifiedContent.Scope> getScopes() {
            return Sets.immutableEnumSet(
                    QualifiedContent.Scope.PROJECT,
                    QualifiedContent.Scope.SUB_PROJECTS,
                    QualifiedContent.Scope.EXTERNAL_LIBRARIES
            )
        }

        @Override
        boolean isIncremental() {
            return true
        }

        @Override
        void transform(TransformInvocation transformInvocation) {
            def openedJar = [:]
            transformInvocation.inputs.each { input ->
                input.directoryInputs.each { dirInput ->
                    def outDir = transformInvocation.outputProvider.getContentLocation(
                            dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                    def inputDir = dirInput.file
                    FileUtils.copyDirectory(inputDir, outDir)
                    inputDir.eachFileRecurse(FileType.FILES) { file ->
                        findTargetClass(file, inputDir, outDir)
                    }
                }
                input.jarInputs.each { jarInput ->
                    def jarFile = jarInput.file
                    def jarName = jarInput.name
                    File unzipDir = new File(jarFile.getParent(),
                            jarName.replace(":", "") + "_unzip")
                    if (!unzipDir.exists()) {
                        unzipDir.mkdirs()
                    }
                    decompress(jarFile, unzipDir)
                    unzipDir.eachFileRecurse(FileType.FILES) { file ->
                        findTargetClass(file, unzipDir, unzipDir)
                    }
                    def dest = transformInvocation.outputProvider.getContentLocation(
                            jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    openedJar[unzipDir] = dest
                }
            }
            logD(TAG,  "========================================")
            logD(TAG,  "start to transform ...")
            logD(TAG,  "----------------------------------------")
            if (null != mAppInfo && null != mAppLikeInfo) {
                handleTargetClasses()
            } else {
                logE(TAG, "Fatal error: failed to find Application and AppLike.")
            }
            logD(TAG,  "----------------------------------------")
            openedJar.each { unzipDir, dest ->
                logD(TAG,  "" + dest + " <-- " + unzipDir.name)
                compress(unzipDir, dest)
                unzipDir.deleteDir()
            }
            logD(TAG,  "========================================")
        }

        private void findTargetClass(File file, File baseDir, File outDir) {
            if (file.exists() && file.name.endsWith(".class")) {
                def fis = new FileInputStream(file)
                ClassReader cr = new ClassReader(fis)
                cr.accept(new ClassVisitor(Opcodes.ASM5) {
                    @Override
                    void visit(int version, int access, String name, String signature,
                               String superName, String[] interfaces) {
                        super.visit(version, access, name, signature, superName, interfaces)
                        if ("com/jjmoo/appjoint/AppLike" == name) {
                            mAppLikeInfo = new ClassInfo(cr.className, file, baseDir, outDir)
                        }
                        if ("com/jjmoo/appjoint/AppJoint" == name) {
                            mModulesInfo = new ClassInfo(cr.className, file, baseDir, outDir)
                        }
                    }

                    @Override
                    AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        switch (desc) {
                            case "Lcom/jjmoo/appjoint/annotation/AppSpec;":
                                if (null == mAppInfo) {
                                    mAppInfo = new ClassInfo(cr.className, file, baseDir, outDir)
                                } else {
                                    throw new IllegalArgumentException(
                                            "there are more than one application classes")
                                }
                                break
                            case "Lcom/jjmoo/appjoint/annotation/ModuleSpec;":
                                mModuleSpecs.add(cr.className)
                                break
                            case "Lcom/jjmoo/appjoint/annotation/ServiceProvider;":
                                cr.interfaces.each {
                                    mModulesServices[it] = cr.className
                                }
                                break
                            default:
                                break
                        }
                        return super.visitAnnotation(desc, visible)
                    }
                }, 0)
                fis.close()
            }
        }

        private void handleTargetClasses() {
            handleAppSpec()
            handleModuleSpec()
            handleServiceProvider()
        }

        private void handleAppSpec() {
            FileInputStream fis = new FileInputStream(mAppInfo.source)
            ClassReader reader = new ClassReader(fis)
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            reader.accept(new AppSpecClassVisitor(writer), 0)
            fis.close()
            mAppInfo.target.bytes = writer.toByteArray()
        }

        private void handleModuleSpec() {
            FileInputStream fis = new FileInputStream(mAppLikeInfo.source)
            ClassReader reader = new ClassReader(fis)
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            reader.accept(new AppLikeClassVisitor(writer), 0)
            fis.close()
            mAppLikeInfo.target.bytes = writer.toByteArray()
        }

        private void handleServiceProvider() {
            FileInputStream fis = new FileInputStream(mModulesInfo.source)
            ClassReader reader = new ClassReader(fis)
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            reader.accept(new ModulesClassVisitor(writer), 0)
            fis.close()
            mModulesInfo.target.bytes = writer.toByteArray()
        }

        private class AppSpecClassVisitor extends ClassVisitor {
            def mMethodDefined = [:]
            def mMethodDesc = [:]

            AppSpecClassVisitor(ClassVisitor cv) {
                super(Opcodes.ASM5, cv)
                mMethodDefined["onCreate"] = false
                mMethodDefined["onLowMemory"] = false
                mMethodDefined["onTerminate"] = false
                mMethodDefined["onTrimMemory"] = false
                mMethodDefined["attachBaseContext"] = false
                mMethodDefined["onConfigurationChanged"] = false
                mMethodDesc["onCreate"] = "()V"
                mMethodDesc["onLowMemory"] = "()V"
                mMethodDesc["onTerminate"] = "()V"
                mMethodDesc["onTrimMemory"] = "(I)V"
                mMethodDesc["attachBaseContext"] = "(Landroid/content/Context;)V"
                mMethodDesc["onConfigurationChanged"] = "(Landroid/content/res/Configuration;)V"
            }

            @Override
            MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                      String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
                if (mMethodDefined.keySet().contains(name)) {
                    mMethodDefined[name] = true
                    return new AppSpecCodeMethodVisitor(mv, name, desc)
                } else {
                    return mv
                }
            }

            @Override
            void visitEnd() {
                mMethodDefined.each { name, defined ->
                    if (!defined) {
                        logD(TAG,  "add lifecycle method: " + name)
                        String desc = mMethodDesc[name]
                        int access = "attachBaseContext" == name ? 4 : 1
                        MethodVisitor mv = visitMethod(access, name, desc, null, null)
                        mv.visitVarInsn(Opcodes.ALOAD, 0)
                        switch (name) {
                            case "attachBaseContext":
                            case "onConfigurationChanged":
                                mv.visitVarInsn(Opcodes.ALOAD, 1)
                                break
                            case "onTrimMemory":
                                mv.visitVarInsn(Opcodes.ILOAD, 1)
                                break
                            default:
                                break
                        }
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                "android/app/Application", name, desc, false)
                        mv.visitInsn(Opcodes.RETURN)
                        mv.visitEnd()
                    }
                }
                super.visitEnd()
            }
        }

        private class AppSpecCodeMethodVisitor extends MethodVisitor {
            private String name
            private String desc

            private AppSpecCodeMethodVisitor(MethodVisitor mv, String name, String desc) {
                super(Opcodes.ASM5, mv)
                this.name = name
                this.desc = desc
            }

            @Override
            void visitInsn(int opcode) {
                if (Opcodes.RETURN == opcode) {
                    logD(TAG,  "insert code to call AppLike's lifecycle: " + name)
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, mAppLikeInfo.name, "getInstance",
                            "()L" + mAppLikeInfo.name + ";", false)
                    switch (name) {
                        case "attachBaseContext":
                        case "onConfigurationChanged":
                            mv.visitVarInsn(Opcodes.ALOAD, 1)
                            break
                        case "onTrimMemory":
                            mv.visitVarInsn(Opcodes.ILOAD, 1)
                            break
                        default:
                            break
                    }
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mAppLikeInfo.name, name, desc, false)
                }
                super.visitInsn(opcode)
            }
        }

        private class AppLikeClassVisitor extends ClassVisitor {
            AppLikeClassVisitor(ClassVisitor cv) {
                super(Opcodes.ASM5, cv)
            }

            @Override
            MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                      String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
                if (name == "<init>" && desc == "()V") {
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        void visitInsn(int opcode) {
                            if (Opcodes.RETURN == opcode) {
                                mModuleSpecs.each { clazz ->
                                    logD(TAG,  "insert code to add module's " +
                                            "application: " + clazz)
                                    mv.visitVarInsn(Opcodes.ALOAD, 0)
                                    mv.visitTypeInsn(Opcodes.NEW, clazz)
                                    mv.visitInsn(Opcodes.DUP)
                                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, clazz,
                                            "<init>", "()V", false)
                                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mAppLikeInfo.name,
                                            "addModuleApplication",
                                            "(Landroid/app/Application;)V", false)
                                }
                            }
                            super.visitInsn(opcode)
                        }
                    }
                }
                return mv
            }
        }

        private class ModulesClassVisitor extends ClassVisitor {
            private boolean mClinitDefined

            ModulesClassVisitor(ClassVisitor cv) {
                super(Opcodes.ASM5, cv)
            }

            @Override
            MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                      String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
                if (name == "<clinit>") {
                    mClinitDefined = true
                    return new MethodVisitor(Opcodes.ASM5, mv) {
                        @Override
                        void visitInsn(int opcode) {
                            if (Opcodes.RETURN == opcode) {
                                mModulesServices.each { i, impl ->
                                    logD(TAG,  "insert code to add module's " +
                                            "service: " + impl)
                                    mv.visitLdcInsn(Type.getObjectType(i))
                                    mv.visitLdcInsn(Type.getObjectType(impl))
                                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, mModulesInfo.name,
                                            "register",
                                            "(Ljava/lang/Class;Ljava/lang/Class;)V", false)
                                }
                            }
                            super.visitInsn(opcode)
                        }
                    }
                }
                return mv
            }
        }

        private class ClassInfo {
            public final String name
            public final File source
            public final File target

            ClassInfo(String name, File source, File baseDir, File outDir) {
                int len = baseDir.toString().length() + 1
                this.name = name
                this.source = source
                this.target = new File(outDir, source.toString().substring(len))
            }

            @Override
            String toString() {
                return name.toString() + ": " + source
            }
        }

        private void logD(String tag, String msg) {
//            mProject.logging.println(tag + msg)
        }

        private void logE(String tag, String msg) {
            mProject.logging.println(tag + msg)
        }

        private static void decompress(File inFile, File outDir) {
            JarFile jarFile = new JarFile(inFile)
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement()
                File outFile = new File(outDir, entry.getName())
                if (!outFile.exists()) {
                    if (entry.isDirectory()) {
                        outFile.mkdirs()
                    } else {
                        outFile.getParentFile().mkdirs()
                    }
                }
                if (!entry.isDirectory()) {
                    ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE)
                    FileChannel outChannel = new FileOutputStream(outFile).getChannel()
                    ReadableByteChannel inChannel = Channels.newChannel(jarFile.getInputStream(entry))
                    while (0 <= inChannel.read(buf)) {
                        buf.flip()
                        outChannel.write(buf)
                        buf.clear()
                    }
                    inChannel.close()
                    outChannel.close()
                }
            }
        }

        private static void compress(File inDir, File outFile) {
            FileOutputStream fos = new FileOutputStream(outFile)
            CheckedOutputStream cos = new CheckedOutputStream(fos, new CRC32())
            ZipOutputStream zos = new ZipOutputStream(cos)
            int baseLen = inDir.toString().length() + 1
            byte [] buf = new byte[BUFFER_SIZE]
            inDir.eachFileRecurse(FileType.FILES) { file ->
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))
                ZipEntry entry = new ZipEntry(file.toString().substring(baseLen))
                zos.putNextEntry(entry)
                while (true) {
                    int len = bis.read(buf)
                    if (len < 0) {
                        break
                    }
                    zos.write(buf, 0, len)
                }
                bis.close()
            }
            zos.flush()
            zos.close()
        }
    }
}