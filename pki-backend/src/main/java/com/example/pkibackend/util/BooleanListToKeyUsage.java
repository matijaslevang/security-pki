package com.example.pkibackend.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;

import java.util.ArrayList;
import java.util.List;

public class BooleanListToKeyUsage {

    public static int getKeyUsageMaskFromBooleanList(List<Boolean> values) {
        int keyUsageMask = 0;
        if (values.get(0)) {
            keyUsageMask |= KeyUsage.digitalSignature;
        }
        if (values.get(1)) {
            keyUsageMask |= KeyUsage.nonRepudiation;
        }
        if (values.get(2)) {
            keyUsageMask |= KeyUsage.keyEncipherment;
        }
        if (values.get(3)) {
            keyUsageMask |= KeyUsage.dataEncipherment;
        }
        if (values.get(4)) {
            keyUsageMask |= KeyUsage.keyAgreement;
        }
        if (values.get(5)) {
            keyUsageMask |= KeyUsage.cRLSign;
        }

        return keyUsageMask;
    }

    public static KeyPurposeId[] getExtKeyUsageMaskFromBooleanList(List<Boolean> values) {
        List<KeyPurposeId> extKeyUsageMask = new ArrayList<>();

        if (values.get(0)) {
            extKeyUsageMask.add(KeyPurposeId.id_kp_serverAuth);
        }
        if (values.get(1)) {
            extKeyUsageMask.add(KeyPurposeId.id_kp_clientAuth);
        }
        if (values.get(2)) {
            extKeyUsageMask.add(KeyPurposeId.id_kp_codeSigning);
        }
        if (values.get(3)) {
            extKeyUsageMask.add(KeyPurposeId.id_kp_emailProtection);
        }
        if (values.get(4)) {
            extKeyUsageMask.add(KeyPurposeId.id_kp_timeStamping);
        }

        return extKeyUsageMask.toArray(new KeyPurposeId[0]);
    }
}
