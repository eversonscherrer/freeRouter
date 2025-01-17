package net.freertr.cry;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import net.freertr.util.logger;

/**
 * rivest cipher 4
 *
 * @author matecsaba
 */
public class cryEncrRc4 extends cryEncrGeneric {

    private Cipher crypter;

    /**
     * create instance
     */
    public cryEncrRc4() {
    }

    /**
     * initialize
     *
     * @param key key
     * @param iv iv
     * @param encrypt mode
     */
    public void init(byte[] key, byte[] iv, boolean encrypt) {
        final String name = "RC4";
        int mode;
        if (encrypt) {
            mode = Cipher.ENCRYPT_MODE;
        } else {
            mode = Cipher.DECRYPT_MODE;
        }
        try {
            SecretKeySpec keyspec = new SecretKeySpec(key, name);
            crypter = Cipher.getInstance(name + "/ECB/NoPadding");
            crypter.init(mode, keyspec);
        } catch (Exception e) {
            logger.exception(e);
        }
    }

    /**
     * get name
     *
     * @return name
     */
    public String getName() {
        return "rc4";
    }

    /**
     * get block size
     *
     * @return size
     */
    public int getBlockSize() {
        return 1;
    }

    /**
     * get key size
     *
     * @return size
     */
    public int getKeySize() {
        return 8;
    }

    /**
     * compute block
     *
     * @param buf buffer
     * @param ofs offset
     * @param siz size
     * @return computed block
     */
    public byte[] compute(byte[] buf, int ofs, int siz) {
        return crypter.update(buf, ofs, siz);
    }

    /**
     * get next iv
     *
     * @return iv
     */
    public byte[] getNextIV() {
        return crypter.getIV();
    }

    /**
     * read iv size
     *
     * @return size in bytes
     */
    public int getIVsize() {
        return getBlockSize();
    }

    /**
     * get tag size
     *
     * @return size
     */
    public int getTagSize() {
        return 0;
    }

    /**
     * authenticate buffer
     *
     * @param buf buffer to use
     * @param ofs offset in buffer
     * @param siz bytes to add
     */
    public void authAdd(byte[] buf, int ofs, int siz) {
    }

}
