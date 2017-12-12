package ir.amv.os.tools.maveninterceptor.interceptor.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.List;

public class DigestListOutputStream extends FilterOutputStream {

    private boolean on = true;

    protected List<MessageDigest> digestList;
    private OutputStream[] copy;

    public DigestListOutputStream(OutputStream stream, List<MessageDigest> digestList, OutputStream... copy) {
        super(stream);
        this.digestList = digestList;
        this.copy = copy;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        if (on) {
            digestList.forEach(digest -> digest.update((byte)b));
            if (copy != null) {
                for (OutputStream outputStream : copy) {
                    outputStream.write(b);
                }
            }
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        if (on) {
            digestList.forEach(digest -> digest.update(b, off, len));
            if (copy != null) {
                for (OutputStream outputStream : copy) {
                    outputStream.write(b, off, len);
                }
            }
        }
    }

}
