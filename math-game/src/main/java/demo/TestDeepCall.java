package demo;

import java.util.Objects;

public class TestDeepCall implements DeepCall {

    public static void main(String[] args) {
        int i = 0;
        while (true) {
            try {
                new TestDeepCall().call1(Objects.toString(i++));
            } finally {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    @Override
    public Object call1(Object param) {
        return call2(param);
    }

    @Override
    public Object call2(Object param) {
        return call3(param);
    }

    @Override
    public Object call3(Object param) {
        return call4(param);
    }

    @Override
    public Object call4(Object param) {
        return param;
    }
}
