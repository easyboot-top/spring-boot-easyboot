package top.easyboot.springboot.authorization.utils;

import top.easyboot.springboot.authorization.Protocol;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;

import top.easyboot.springboot.authorization.interfaces.http.Headers;

public class HttpUtils {

    private static final String DEFAULT_ENCODING = "UTF-8";
    private static BitSet URI_UNRESERVED_CHARACTERS = new BitSet();
    private static String[] PERCENT_ENCODED_STRINGS = new String[256];
    public static String urlEncode(String value) {
        try {
            StringBuilder builder = new StringBuilder();
            byte[] var2 = value.getBytes("UTF-8");
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                byte b = var2[var4];
                if (URI_UNRESERVED_CHARACTERS.get(b & 255)) {
                    builder.append((char)b);
                } else {
                    builder.append(PERCENT_ENCODED_STRINGS[b & 255]);
                }
            }

            return builder.toString();
        } catch (UnsupportedEncodingException var6) {
            throw new RuntimeException(var6);
        }
    }

    static {
        /*
         * StringBuilder pattern = new StringBuilder();
         *
         * pattern .append(Pattern.quote("+")) .append("|") .append(Pattern.quote("*")) .append("|")
         * .append(Pattern.quote("%7E")) .append("|") .append(Pattern.quote("%2F"));
         *
         * ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern.toString());
         */
        for (int i = 'a'; i <= 'z'; i++) {
            URI_UNRESERVED_CHARACTERS.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            URI_UNRESERVED_CHARACTERS.set(i);
        }
        for (int i = '0'; i <= '9'; i++) {
            URI_UNRESERVED_CHARACTERS.set(i);
        }
        URI_UNRESERVED_CHARACTERS.set('-');
        URI_UNRESERVED_CHARACTERS.set('.');
        URI_UNRESERVED_CHARACTERS.set('_');
        URI_UNRESERVED_CHARACTERS.set('~');

        for (int i = 0; i < PERCENT_ENCODED_STRINGS.length; ++i) {
            PERCENT_ENCODED_STRINGS[i] = String.format("%%%02X", i);
        }

    }

    /**
     * Normalize a string for use in url path. The algorithm is:
     * <p>
     *
     * <ol>
     *   <li>Normalize the string</li>
     *   <li>replace all "%2F" with "/"</li>
     *   <li>replace all "//" with "/%2F"</li>
     * </ol>
     *
     * <p>
     * Bos object key can contain arbitrary characters, which may result double slash in the url path. Apache http
     * client will replace "//" in the path with a single '/', which makes the object key incorrect. Thus we replace
     * "//" with "/%2F" here.
     *
     * @param path the path string to normalize.
     * @return the normalized path string.
     * @see #normalize(String)
     */
    public static String normalizePath(String path) {
        return normalize(path).replace("%2F", "/");
    }

    /**
     * Normalize a string for use in BCE web service APIs. The normalization algorithm is:
     * <p>
     * <ol>
     *   <li>Convert the string into a UTF-8 byte array.</li>
     *   <li>Encode all octets into percent-encoding, except all URI unreserved characters per the RFC 3986.</li>
     * </ol>
     *
     * <p>
     * All letters used in the percent-encoding are in uppercase.
     *
     * @param value the string to normalize.
     * @return the normalized string.
     */
    public static String normalize(String value) {
        try {
            StringBuilder builder = new StringBuilder();
            for (byte b : value.getBytes(DEFAULT_ENCODING)) {
                if (URI_UNRESERVED_CHARACTERS.get(b & 0xFF)) {
                    builder.append((char) b);
                } else {
                    builder.append(PERCENT_ENCODED_STRINGS[b & 0xFF]);
                }
            }
            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encode a string for use in the path of a URL; uses URLEncoder.encode, (which encodes a string for use in the
     * query portion of a URL), then applies some postfilters to fix things up per the RFC. Can optionally handle
     * strings which are meant to encode a path (ie include '/'es which should NOT be escaped).
     *
     * @param value the value to encode
     * @param path true if the value is intended to represent a path
     * @return the encoded value
     */
    /*
     * public static String urlEncode(String value) { if (value == null) { return ""; }
     *
     * try { String encoded = URLEncoder.encode(value, DEFAULT_ENCODING);
     *
     * Matcher matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded); StringBuffer buffer = new
     * StringBuffer(encoded.length());
     *
     * while (matcher.find()) { String replacement = matcher.group(0);
     *
     * if ("+".equals(replacement)) { replacement = "%20"; } else if ("*".equals(replacement)) { replacement = "%2A"; }
     * else if ("%7E".equals(replacement)) { replacement = "~"; } else if (path && "%2F".equals(replacement)) {
     * replacement = "/"; }
     *
     * matcher.appendReplacement(buffer, replacement); }
     *
     * matcher.appendTail(buffer); return buffer.toString();
     *
     * } catch (UnsupportedEncodingException ex) { throw new RuntimeException(ex); } }
     */

    /**
     * Returns a host header according to the specified URI. The host header is generated with the same logic used by
     * apache http client, that is, append the port to hostname only if it is not the default port.
     *
     * @param uri the URI
     * @return a host header according to the specified URI.
     */
    public static String generateHostHeader(URI uri) {
        String host = uri.getHost();
        if (isUsingNonDefaultPort(uri)) {
            host += ":" + uri.getPort();
        }
        return host;
    }

    /**
     * Returns true if the specified URI is using a non-standard port (i.e. any port other than 80 for HTTP URIs or any
     * port other than 443 for HTTPS URIs).
     *
     * @param uri the URI
     * @return True if the specified URI is using a non-standard port, otherwise false.
     */
    public static boolean isUsingNonDefaultPort(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        int port = uri.getPort();
        if (port <= 0) {
            return false;
        }
        if (scheme.equals(Protocol.HTTP.toString())) {
            return port != Protocol.HTTP.getDefaultPort();
        }
        if (scheme.equals(Protocol.HTTPS.toString())) {
            return port != Protocol.HTTPS.getDefaultPort();
        }
        return false;
    }

    public static String getCanonicalURIPath(String path) {
        if (path == null) {
            return "/";
        } else if (path.startsWith("/")) {
            return normalizePath(path);
        } else {
            return "/" + normalizePath(path);
        }
    }

    public static String getCanonicalQueryString(String query, Set<String> noSignQueryKeys) {
        if (query == null || query.equals("")) {
            return "";
        }
        List<String> parameterStrings = new ArrayList();
        if (query != null && !query.equals("")) {
            for (String t : query.split("&")) {
                int index = t.indexOf("=");
                String key = t.substring(0, index);
                String value = t.substring(index + 1);
                if (key == null) {
                    throw new NullPointerException("parameter key should not be null");
                }
                if (noSignQueryKeys!= null && noSignQueryKeys.contains(key.toLowerCase())){
                    continue;
                }
                if (Headers.AUTHORIZATION.equalsIgnoreCase(key)) {
                    continue;
                }
                if (value == null) {
                    parameterStrings.add(normalize(key) + '=');
                } else {
                    parameterStrings.add(normalize(key) + '=' + normalize(value));
                }
            }
        }
        Collections.sort(parameterStrings);
        return String.join("&", parameterStrings);
    }
    public static String getCanonicalHeaders(Map<String, String> headers, Set<String> noSignHeadersKeys) {
        return getCanonicalHeaders(headers, noSignHeadersKeys, new HashSet<>());
    }
    public static String getCanonicalHeaders(Map<String, String> headers, Set<String> noSignHeadersKeys,Set<String> signedHeaders) {
        if (headers.isEmpty()) {
            return "";
        }

        List<String> headerStrings = new ArrayList();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            String value = entry.getValue();
            if (value == null) {
                value = "";
            }
            if (noSignHeadersKeys!= null && noSignHeadersKeys.contains(key.toLowerCase())){
                continue;
            }
            if (Headers.AUTHORIZATION.equalsIgnoreCase(key)) {
                continue;
            }
            signedHeaders.add(key.trim());
            headerStrings.add(normalize(key.trim().toLowerCase()) + ':' + normalize(value.trim()));
        }
        Collections.sort(headerStrings);

        return  String.join("\n", headerStrings);
    }

}
