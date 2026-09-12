import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.util.HexFormat;
import java.util.jar.JarFile;

/** Verify every payload entry against an independently exported upload certificate. */
class VerifySignedBundle {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: AAB public-certificate.pem");
        byte[] expected;
        try (InputStream certificate = Files.newInputStream(Path.of(args[1]))) {
            expected = CertificateFactory.getInstance("X.509").generateCertificate(certificate).getEncoded();
        }
        int payloads = 0;
        try (JarFile jar = new JarFile(args[0], true)) {
            var entries = jar.entries();
            byte[] buffer = new byte[65536];
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                // Consuming every entry triggers JarFile's cryptographic verification.
                try (InputStream input = jar.getInputStream(entry)) {
                    while (input.read(buffer) != -1) { }
                }
                if (entry.getName().startsWith("META-INF/")) continue;
                var signers = entry.getCodeSigners();
                if (signers == null || signers.length != 1 ||
                    !MessageDigest.isEqual(expected,
                        signers[0].getSignerCertPath().getCertificates().get(0).getEncoded())) {
                    throw new SecurityException("Unsigned or wrong signer: " + entry.getName());
                }
                payloads++;
            }
        }
        if (payloads == 0) throw new SecurityException("No signed bundle payloads");
        System.out.println("Verified payload entries: " + payloads);
        System.out.println("Upload certificate SHA-256: " + HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(expected)));
    }
}
