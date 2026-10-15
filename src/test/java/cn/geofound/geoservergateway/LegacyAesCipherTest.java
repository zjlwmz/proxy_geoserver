package cn.geofound.geoservergateway;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyAesCipherTest {

    @Test
    void decryptsTheExistingCndParamsWireFormat() throws Exception {
        LegacyAesCipher cipher = new LegacyAesCipher("qP2$bG9;vA0^uW0:");
        String plainText = "QWEASDZXC";

        assertThat(cipher.decrypt("vlgQpBdfVvMplxwOTjxSyQ==")).isEqualTo(plainText);
        assertThat(cipher.encrypt(plainText)).isEqualTo("vlgQpBdfVvMplxwOTjxSyQ==");
    }
}
