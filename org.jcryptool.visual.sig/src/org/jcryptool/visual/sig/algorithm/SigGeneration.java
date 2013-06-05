package org.jcryptool.visual.sig.algorithm;

import java.security.*;

import de.flexiprovider.core.rsa.RSAPrivateCrtKey;
import de.flexiprovider.core.dsa.*;
import java.util.Enumeration;
import java.util.HashMap;
import org.jcryptool.core.logging.utils.LogUtil;
import org.jcryptool.crypto.keystore.KeyStorePlugin;
import org.jcryptool.crypto.keystore.backend.KeyStoreAlias;
import org.jcryptool.crypto.keystore.backend.KeyStoreManager;
import org.jcryptool.crypto.keystore.exceptions.NoKeyStoreFileException;

public class SigGeneration {
	public String signature;
	//private final static HashMap<String, KeyStoreAlias> keystoreitems = new HashMap<String, KeyStoreAlias>();
	private static PrivateKey k = null;
	
	/**
	 * Old version of SignInput, calls new version of the method
	 */
	public static byte[] SignInput(String signaturemethod, byte[] input) throws Exception {
		
		return SignInput(signaturemethod, input, null);
	}
	
	/**
	 * This method signs the data stored in Input.java with the signature method selected by the user. 
	 * It either uses the user selected key or the key given by jctca (stored in Input.java).
	 * 
	 * @param signaturemethod Chosen signature method to sign the hash.
	 * @param input The Data the user selected
	 * @return The signature (byte array)
	 * @throws Exception
	 */
	public static byte[] SignInput(String signaturemethod, byte[] input, KeyStoreAlias alias) throws Exception {
		KeyStoreManager ksm = KeyStoreManager.getInstance();
		ksm.loadKeyStore(KeyStorePlugin.getPlatformKeyStoreURI());
		//Check if called by JCT-CA
		if (org.jcryptool.visual.sig.algorithm.Input.privateKey != null) { //Use their key
			org.jcryptool.visual.sig.algorithm.Input.privateKey.getAliasString();
			k = ksm.getPrivateKey(org.jcryptool.visual.sig.algorithm.Input.privateKey, KeyStoreManager.getDefaultKeyPassword());
		} else { //Use Key from given alias
			k = ksm.getPrivateKey(alias, KeyStoreManager.getDefaultKeyPassword());
			//org.jcryptool.visual.sig.algorithm.Input.privateKey = (KeyStoreAlias) k;
		}
		
		byte[] signature = null; //Stores the signature
		
		// Get a signature object using the specified combo and sign the data with the private key
		Signature sig = Signature.getInstance(signaturemethod);
        //sig.initSign(key.getPrivate());
		sig.initSign(k);
        sig.update(input);
        signature = sig.sign();
        
        //Test
//        System.out.println( "\nStart signature verification" );
//        sig.initVerify(ksm.getPublicKey(alias));
//        sig.update(input);
//        try {
//            if (sig.verify(signature)) {
//                System.out.println( "Signature verified" );
//            } else System.out.println( "Signature failed" );
//        } catch (SignatureException se) {
//            System.out.println( "Signature failed" );
//        }
        
        //Store the generated signature
        org.jcryptool.visual.sig.algorithm.Input.signature = signature; //Store the generated original signature
	    org.jcryptool.visual.sig.algorithm.Input.signatureHex = org.jcryptool.visual.sig.algorithm.Input.bytesToHex(signature); //Hex String
	    org.jcryptool.visual.sig.algorithm.Input.signatureOct = org.jcryptool.visual.sig.algorithm.Input.toOctalString(signature, "");
//	    org.jcryptool.visual.sig.algorithm.Input.signatureDec = org.jcryptool.visual.sig.algorithm.Input.toHexString(signature);
	    return signature;		
	}
	
	
	//Obsolete!***************************************************************************************************
    /**
     * initializes the connection to the keystore.
     * @throws Exception 
     */
	/*
    private static PrivateKey initKeystoreRSA() throws Exception {
        KeyStoreManager ksm = KeyStoreManager.getInstance();
        PrivateKey k = null;
        try {
            ksm.loadKeyStore(KeyStorePlugin.getPlatformKeyStoreURI());
            KeyStoreAlias alias;
            Enumeration<String> aliases = ksm.getAliases();
            while (aliases != null && aliases.hasMoreElements()) {
                alias = new KeyStoreAlias(aliases.nextElement());
                alias.getAliasString();
                //k1 = ksm.getKey(alias); 
                if (alias.getClassName().equals(RSAPrivateCrtKey.class.getName())) {
                    keystoreitems.put(alias.getContactName() + " - " + alias.getKeyLength() + "Bit - " //$NON-NLS-1$ //$NON-NLS-2$
                            + alias.getClassName(), alias);
                    k = ksm.getPrivateKey(alias, KeyStoreManager.getDefaultKeyPassword());
                } 
            }
            return k;
        } catch (NoKeyStoreFileException e) {
            LogUtil.logError(e);
        } catch (KeyStoreException e) {
            LogUtil.logError(e);
        }
		return k;
    }
    
    private static PrivateKey initKeystoreDSA() throws Exception {
        KeyStoreManager ksm = KeyStoreManager.getInstance();
        PrivateKey k = null;
        try {
            ksm.loadKeyStore(KeyStorePlugin.getPlatformKeyStoreURI());
            KeyStoreAlias alias;
            Enumeration<String> aliases = ksm.getAliases();
            while (aliases != null && aliases.hasMoreElements()) {
                alias = new KeyStoreAlias(aliases.nextElement());
                alias.getAliasString();
                //k1 = ksm.getKey(alias); 
                if (alias.getClassName().equals(DSAPrivateKey.class.getName())) {
                    keystoreitems.put(alias.getContactName() + " - " + alias.getKeyLength() + "Bit - " //$NON-NLS-1$ //$NON-NLS-2$
                            + alias.getClassName(), alias);
                    k = ksm.getPrivateKey(alias, KeyStoreManager.getDefaultKeyPassword());
                } 
            }
            return k;
        } catch (NoKeyStoreFileException e) {
            LogUtil.logError(e);
        } catch (KeyStoreException e) {
            LogUtil.logError(e);
        }
		return k;
    }
    */
    //Obsolete!***************************************************************************************************
}
