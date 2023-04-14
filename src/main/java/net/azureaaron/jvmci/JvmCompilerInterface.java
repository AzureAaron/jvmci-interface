package net.azureaaron.jvmci;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.site.DataPatch;
import jdk.vm.ci.code.site.Site;
import jdk.vm.ci.hotspot.HotSpotCompiledCode.Comment;
import jdk.vm.ci.hotspot.HotSpotCompiledNmethod;
import jdk.vm.ci.hotspot.HotSpotResolvedJavaMethod;
import jdk.vm.ci.meta.Assumptions.Assumption;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.runtime.JVMCI;
import jdk.vm.ci.runtime.JVMCIBackend;
import net.azureaaron.jvmci.calling.CallingConvention;

public class JvmCompilerInterface {
	private static final JVMCIBackend JVMCI_BACKEND = JVMCI.getRuntime().getHostJVMCIBackend();

	public static void installFunction(Method method, long address) {
		CallingConvention callingConvention = CallingConvention.getInstance();
		ByteBuffer buf = ByteBuffer.allocate(100).order(ByteOrder.nativeOrder());
		callingConvention.javaToNative(buf, method.getParameterTypes(), method.getParameterAnnotations());
		callingConvention.emitCall(buf, address);
		try {
			 installCode(method, JvmCompilerInterface.assembleToByteArray(buf));
		} catch (Throwable t) {
			throw new RuntimeException("Failed to install code", t);
		}
	}

	private static byte[] assembleToByteArray(ByteBuffer buf) {
		return buf.array();
	}

	private static void installCode(Method method, byte[] code) {
		ResolvedJavaMethod resolvedMethod = JVMCI_BACKEND.getMetaAccess().lookupJavaMethod(method);
		HotSpotCompiledNmethod compiledCode = new HotSpotCompiledNmethod(method.getName(), code, code.length,
				new Site[0], new Assumption[0], new ResolvedJavaMethod[0],
				new Comment[0], new byte[0], 1, new DataPatch[0], true, 0, null,
				(HotSpotResolvedJavaMethod) resolvedMethod, -1, 1, 0L, false);
		CodeCacheProvider codeCache = JVMCI_BACKEND.getCodeCache();
		codeCache.setDefaultCode(resolvedMethod, compiledCode);
	}
}
