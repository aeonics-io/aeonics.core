package aeonics.util;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class offers simple methods to protect a method based on the calling method.
 * You can either require or prevent another class to call a your function.
 * 
 * <p>Example to make sure the specified method is in the call stack:</p>
 * <pre>public void foo() {
 *     CheckCaller.require(Example.class, "bar");
 * }</pre>
 * 
 * <p>While this is not a bulletproof security system, it is safe enough to be used as intended.
 * However, in case unsafe reflection, bytecode manipulations, java agents, or debuggers are into play,
 * the call stack trace can be manipulated or spoofed to mislead this implementation.</p>
 */
public class CheckCaller 
{
	/**
	 * Calls {@link #require(Class, String, int, boolean)} with an unlimited depth and not strict check.
	 * @see #require(Class, String, int, boolean)
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 */
	public static void require(Class<?> clazz, String method) { require(clazz, method, Integer.MAX_VALUE, false); }
	
	/**
	 * Calls {@link #require(Class, String, int, boolean)} with an unlimited depth.
	 * @see #require(Class, String, int, boolean)
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 * @param strict whether or not to check in strict mode
	 */
	public static void require(Class<?> clazz, String method, boolean strict) { require(clazz, method, Integer.MAX_VALUE, strict); }
	
	/**
	 * Calls {@link #require(Class, String, int, boolean)} in non strict mode.
	 * @see #require(Class, String, int, boolean)
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 * @param maxDepth the maximum intermediate method calls
	 */
	public static void require(Class<?> clazz, String method, int maxDepth) { require(clazz, method, maxDepth, false); }
	
	/**
	 * Checks that the current method has been called as a result of the specified class and method.
	 * <p>In other words:</p>
	 * <pre>
	 * class Test {
	 *     public void foo() {
	 *         CheckCaller.require(Example.class, "bar");
	 *     }
	 * }
	 * </pre>
	 * <p>This code sample will check that the <code>foo()</code> method has been called
	 * from <code>Example.bar()</code> somewhere in the hyerarchy, it may be a direct call or
	 * an indirect call.</p>
	 * 
	 * <p>If the <b>method</b> is null, then it is not checked and any method of the specified class
	 * is considered.</p>
	 * 
	 * <p>If the <b>strict</b> mode is enabled, then only the specified class is valid. Otherwise
	 * any other subclass (as defined by {@link Class#isAssignableFrom(Class)}) is also valid.</p>
	 * 
	 * <p>The <b>maxDepth</b> parameted defines the number of intermediate method calls allowed. In other words,
	 * the depth of the stack trace. The depth count starts as <code>0</code> for the direct caller:</p>
	 * <pre>
	 * public void foo() {
	 *     
	 *     // this will check that the direct caller (depth 0) 
	 *     // is exactly the Example class and no other subclass.
	 *     
	 *     CheckCaller.require(Example.class, "bar", 0, true);
	 * }
	 * </pre>
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 * @param maxDepth the maximum intermediate method calls
	 * @param strict whether or not to check in strict mode
	 */
	public static void require(Class<?> clazz, String method, int maxDepth, boolean strict)
	{
		if( !hasCaller(clazz, method, maxDepth, strict) )
			throw new IllegalCallerException();
	}
	
	/**
	 * Calls {@link #prevent(Class, String, int, boolean)} with an unlimited depth and not strict check.
	 * @see #prevent(Class, String, int, boolean)
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 */
	public static void prevent(Class<?> clazz, String method) { require(clazz, method, Integer.MAX_VALUE, false); }
	
	/**
	 * Calls {@link #prevent(Class, String, int, boolean)} with an unlimited depth.
	 * @see #prevent(Class, String, int, boolean)
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 * @param strict whether or not to check in strict mode
	 */
	public static void prevent(Class<?> clazz, String method, boolean strict) { require(clazz, method, Integer.MAX_VALUE, strict); }
	
	/**
	 * Calls {@link #prevent(Class, String, int, boolean)} in non strict mode.
	 * @see #prevent(Class, String, int, boolean)
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 * @param maxDepth the maximum intermediate method calls
	 */
	public static void prevent(Class<?> clazz, String method, int maxDepth) { require(clazz, method, maxDepth, false); }
	
	/**
	 * Checks that the current method has <b>NOT</b> been called as a result of the specified class and method.
	 * <p>In other words:</p>
	 * <pre>
	 * class Test {
	 *     public void foo() {
	 *         CheckCaller.prevent(Example.class, "bar");
	 *     }
	 * }
	 * </pre>
	 * <p>This code sample will check that the <code>foo()</code> method has not been called
	 * from <code>Example.bar()</code> somewhere in the hyerarchy, either by a direct call or
	 * an indirect call.</p>
	 * 
	 * <p>If the <b>method</b> is null, then it is not checked and any method of the specified class
	 * is considered.</p>
	 * 
	 * <p>If the <b>strict</b> mode is enabled, then only the specified class is valid. Otherwise
	 * any other subclass (as defined by {@link Class#isAssignableFrom(Class)}) is also valid.</p>
	 * 
	 * <p>The <b>maxDepth</b> parameted defines the number of intermediate method calls allowed. In other words,
	 * the depth of the stack trace. The depth count starts as <code>0</code> for the direct caller:</p>
	 * <pre>
	 * public void foo() {
	 *     
	 *     // this will check that the direct caller (depth 0) 
	 *     // is not the Example class nor any of its subclasses.
	 *     // Although it would allow Example to be further in the hyerarchy.
	 *     
	 *     CheckCaller.prevent(Example.class, "bar", 0, false);
	 * }
	 * </pre>
	 * @param clazz the class to check
	 * @param method the method to check (may be null)
	 * @param maxDepth the maximum intermediate method calls
	 * @param strict whether or not to check in strict mode
	 */
	public static void prevent(Class<?> clazz, String method, int maxDepth, boolean strict)
	{
		if( hasCaller(clazz, method, maxDepth, strict) )
			throw new IllegalCallerException();
	}
	
	private static boolean hasCaller(Class<?> clazz, String method, int maxDepth, boolean strict)
	{
		StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		AtomicInteger depth = new AtomicInteger(-1); // start at -1 because frame 0 is the caller of this method and should be excluded
		Optional<StackWalker.StackFrame> valid = walker.walk(frames -> 
			frames.dropWhile(frame -> frame.getDeclaringClass().equals(CheckCaller.class))
				.filter(frame -> depth.getAndIncrement() <= maxDepth)
				.filter(frame -> {
	                if (strict) {
	                    return frame.getDeclaringClass().equals(clazz) && (method == null || frame.getMethodName().equals(method));
	                } else {
	                    return clazz.isAssignableFrom(frame.getDeclaringClass()) && (method == null || frame.getMethodName().equals(method));
	                }
	            })
				.findFirst()
		);
		return valid.isPresent();
	}
}
