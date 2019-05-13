import java.lang.reflect.Array;
import java.util.Arrays;

public class Tools {
    static public byte[] concatenate(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;

        byte[] c = (byte[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    static public byte[] deleteNullBytes(byte[] input){
        int i = input.length - 1;
        while (i >= 0 && input[i] == 0)
            --i;
        return Arrays.copyOf(input, i + 1);
    }
}
