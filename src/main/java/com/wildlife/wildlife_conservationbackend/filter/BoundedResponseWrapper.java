package com.wildlife.wildlife_conservationbackend.filter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

final class BoundedResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private final int limit;
    private ServletOutputStream stream;
    private PrintWriter writer;
    private boolean streamSelected;
    private boolean truncated;

    BoundedResponseWrapper(HttpServletResponse response, int limit) {
        super(response);
        this.limit = limit;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called.");
        }
        streamSelected = true;
        return captureStream();
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (streamSelected) {
            throw new IllegalStateException("getOutputStream() has already been called.");
        }
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(captureStream(), Charset.forName(getCharacterEncoding())));
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        finishCapture();
        super.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        clearCapture();
    }

    @Override
    public void reset() {
        super.reset();
        clearCapture();
        writer = null;
        stream = null;
        streamSelected = false;
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        clearCapture();
        super.sendError(status, message);
    }

    @Override
    public void sendError(int status) throws IOException {
        clearCapture();
        super.sendError(status);
    }

    void finishCapture() {
        if (writer != null) {
            writer.flush();
        }
    }

    byte[] getContentAsByteArray() {
        return captured.toByteArray();
    }

    boolean isTruncated() {
        return truncated;
    }

    private void clearCapture() {
        captured.reset();
        truncated = false;
    }

    private ServletOutputStream captureStream() throws IOException {
        if (stream == null) {
            ServletOutputStream delegate = super.getOutputStream();
            stream = new ServletOutputStream() {
                @Override
                public void write(int value) throws IOException {
                    delegate.write(value);
                    if (captured.size() < limit) {
                        captured.write(value);
                    } else {
                        truncated = true;
                    }
                }

                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    delegate.write(bytes, offset, length);
                    int remaining = limit - captured.size();
                    captured.write(bytes, offset, Math.min(length, remaining));
                    truncated |= length > remaining;
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                    delegate.setWriteListener(listener);
                }

                @Override
                public void flush() throws IOException {
                    delegate.flush();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }
            };
        }
        return stream;
    }
}
