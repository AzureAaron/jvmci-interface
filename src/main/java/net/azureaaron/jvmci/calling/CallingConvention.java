package net.azureaaron.jvmci.calling;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.runtime.JVMCI;

public abstract class CallingConvention {

    public static CallingConvention getInstance() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (!arch.contains("64")) {
            throw new IllegalStateException("Unsupported architecture: " + arch);
        }

        if (arch.contains("aarch") || arch.contains("arm")) {
            return new AArch64CallingConvention();
        }

        if (arch.contains("riscv")) {
            return new RISCV64CallingConvention();
        }

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return new AMD64WindowsCallingConvention();
        } else {
            return new AMD64LinuxCallingConvention();
        }
    }

    public abstract void javaToNative(ByteBuffer buf, Class<?>[] types, Annotation[][] annotations);

    public abstract void emitCall(ByteBuffer buf, long address);

    protected static int baseOffset(Class<?> type, Annotation[] annotations) {
        if (type.isArray() && type.getComponentType().isPrimitive()) {
            return arrayBaseOffset(type);
        }

        throw new IllegalArgumentException("Unsupported argument type: " + type);
    }

    protected static int arrayBaseOffset(Class<?> arrayType) {
        MetaAccessProvider meta = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
        JavaKind elementKind = JavaKind.fromJavaClass(arrayType.getComponentType());
        return meta.getArrayBaseOffset(elementKind);
    }

    protected static int fieldOffset(Class<?> type, String fieldName) {
        MetaAccessProvider meta = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
        ResolvedJavaField[] fields = meta.lookupJavaType(type).getInstanceFields(true);
        if (fields == null || fields.length == 0) {
            throw new IllegalArgumentException(type.getName() + " does not have instance fields");
        }

        if (fieldName.isEmpty()) {
            return fields[0].getOffset();
        }

        for (ResolvedJavaField field : fields) {
            if (field.getName().equals(fieldName)) {
                return field.getOffset();
            }
        }
        throw new IllegalArgumentException("No such field: " + type.getName() + "." + fieldName);
    }

    protected static byte asByte(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Not in the byte range: " + value);
        }
        return (byte) value;
    }

    protected static void emit(ByteBuffer buf, int code) {
        if ((code >>> 24) != 0) buf.put((byte) (code >>> 24));
        if ((code >>> 16) != 0) buf.put((byte) (code >>> 16));
        if ((code >>> 8) != 0) buf.put((byte) (code >>> 8));
        if (code != 0) buf.put((byte) code);
    }
}
