package com.auction.server.socket;

import java.io.PrintWriter;

public class TcpSocketClient implements SocketClient {
    private final PrintWriter out;

    public TcpSocketClient(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void sendMessage(String message) {
        try {
            out.println(message);
            out.flush();
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public boolean isOpen() {
        return out != null && !out.checkError();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TcpSocketClient that = (TcpSocketClient) o;
        return java.util.Objects.equals(out, that.out);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(out);
    }
}
