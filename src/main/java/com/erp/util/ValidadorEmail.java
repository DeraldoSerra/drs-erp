package com.erp.util;

import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.regex.Pattern;

public class ValidadorEmail {

    private static final Pattern REGEX_EMAIL = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    /** Validates email format using regex. */
    public static boolean validarFormato(String email) {
        if (email == null || email.isBlank()) return false;
        return REGEX_EMAIL.matcher(email.trim()).matches();
    }

    /**
     * Checks if the domain has at least one MX record via DNS lookup.
     * Returns true if check passes OR if DNS is unavailable (graceful degradation).
     * Must be called in a background thread.
     */
    public static boolean verificarDNS(String email) {
        if (!validarFormato(email)) return false;
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase().trim();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "1");
            DirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});
            ctx.close();
            return attrs.get("MX") != null;
        } catch (javax.naming.NameNotFoundException e) {
            // Domain does not exist
            return false;
        } catch (Exception e) {
            // DNS unavailable or other error — fail open (don't block the user)
            return true;
        }
    }
}
