/**
 * Various IP address utils
 *
 * @author Charles Johnson
 */
public class IpConverter {

    /**
     * Convert an IP address to a hex string
     *
     * @param ipAddress Input IP address
     *
     * @return The IP address in hex form
     */
    public static String toHex(String ipAddress) {
        return Long.toHexString(IpConverter.ipToLong(ipAddress));
    }

    /**
     * Convert an IP address to a number
     *
     * @param ipAddress Input IP address
     *
     * @return The IP address as a number
     */
    private static long ipToLong(String ipAddress) {
        long result = 0;
        String[] atoms = ipAddress.split("\\.");

        for (int i = 3; i >= 0; i--) {
            result |= (Long.parseLong(atoms[3 - i]) << (i * 8));
        }

        return result & 0xFFFFFFFF;
    }

    private static String longToIp(long ip) {
        StringBuilder sb = new StringBuilder(15);

        for (int i = 0; i < 4; i++) {
            sb.insert(0, Long.toString(ip & 0xff));

            if (i < 3) {
                sb.insert(0, '.');
            }

            ip >>= 8;
        }

        return sb.toString();
    }

    static String next(String ip) {
        return longToIp(ipToLong(ip)+1L);
    }
}