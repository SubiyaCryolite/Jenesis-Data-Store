import common.BaseTestConfig;
import io.github.subiyacryolite.jds.JdsEntity;
import javafx.beans.property.SimpleBlobProperty;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Created by indana on 5/10/2017.
 */
public class SerializationTests extends BaseTestConfig {

    @Test
    public void testSerialization() {
        initialiseSqlLiteBackend();
        for (Class<? extends JdsEntity> jdsEntity : jdsDb.getMappedClasses()) {
            String canonicalName = jdsEntity.getCanonicalName();
            serialize(jdsEntity, canonicalName);
            deserialize(canonicalName, jdsEntity);
        }
    }

    @Test
    public void testBlobSerialization() {
        SimpleBlobProperty simpleBlobProperty = new SimpleBlobProperty(new byte[]{(byte)0xC9, (byte)0xCB, (byte)0xBB,
                (byte)0xCC, (byte)0xCE, (byte)0xB9,
                (byte)0xC8, (byte)0xCA, (byte)0xBC,
                (byte)0xCC, (byte)0xCE, (byte)0xB9,
                (byte)0xC9, (byte)0xCB, (byte)0xBB});
        String canonicalName = simpleBlobProperty.getClass().getCanonicalName();
        serialize(simpleBlobProperty, canonicalName);
        SimpleBlobProperty out = deserialize(canonicalName, simpleBlobProperty.getClass());
        System.out.printf("pre %s\n",Arrays.toString(simpleBlobProperty.get()));
        System.out.printf("post %s\n",Arrays.toString(out.get()));
    }
}
