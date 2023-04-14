package net.azureaaron.jvmci.calling;

import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;

public class AMD64WindowsCallingConvention extends CallingConvention {

    // x64 calling convention (Windows):
    //     Java: rdx,  r8,  r9, rdi, rsi, rcx, stack
    //   Native: rcx, rdx,  r8,  r9, stack

    private static final int[] MOVE_INT_ARG = {
            0x89d1,    // mov  ecx, edx
            0x4489c2,  // mov  edx, r8d
            0x4589c8,  // mov  r8d, r9d
            0x4189f9,  // mov  r9d, edi
    };

    private static final int[] MOVE_LONG_ARG = {
            0x4889d1,  // mov  rcx, rdx
            0x4c89c2,  // mov  rdx, r8
            0x4d89c8,  // mov  r8, r9
            0x4989f9,  // mov  r9, rdi
    };

    private static final int[] MOVE_OBJ_ARG = {
            0x488d4a,  // lea  rcx, [rdx+N]
            0x498d50,  // lea  rdx, [r8+N]
            0x4d8d41,  // lea  r8, [r9+N]
            0x4c8d4f,  // lea  r9, [rdi+N]
    };

    @Override
    public void javaToNative(ByteBuffer buf, Class<?>[] types, Annotation[][] annotations) {
        int index = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if (type == float.class || type == double.class) {
                continue;
            }
            if (index >= 4) {
                throw new IllegalArgumentException("At most 4 integer arguments are supported");
            } else if (type.isPrimitive()) {
                emit(buf, (type == long.class ? MOVE_LONG_ARG : MOVE_INT_ARG)[index++]);
            } else {
                emit(buf, MOVE_OBJ_ARG[index++]);
                buf.put(asByte(baseOffset(type, annotations[i])));
            }
        }
    }

    @Override
    public void emitCall(ByteBuffer buf, long address) {
        buf.putShort((short) 0xb848).putLong(address);  // mov rax, address
        buf.putShort((short) 0xe0ff);                   // jmp rax
    }
}
