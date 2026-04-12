package com.erp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Assina digitalmente o XML da NF-e com certificado A1 (PKCS12/.pfx).
 * Usa exclusivamente javax.xml.crypto.dsig do Java padrão.
 */
public class NFeAssinador {

    private static final Logger log = LoggerFactory.getLogger(NFeAssinador.class);

    private final String pfxPath;
    private final String pfxSenha;

    public NFeAssinador(String pfxPath, String pfxSenha) {
        this.pfxPath = pfxPath;
        this.pfxSenha = pfxSenha;
    }

    /**
     * Assina o XML da NF-e no elemento infNFe identificado pelo Id="NFe{chave}".
     *
     * @param xmlNfe XML da NF-e sem assinatura
     * @return XML assinado como String
     * @throws Exception em caso de falha
     */
    public String assinar(String xmlNfe) throws Exception {
        // Carrega o keystore
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var fis = new java.io.FileInputStream(pfxPath)) {
            ks.load(fis, pfxSenha.toCharArray());
        }

        // Obtém o alias com chave privada
        String alias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String a = aliases.nextElement();
            if (ks.isKeyEntry(a)) { alias = a; break; }
        }
        if (alias == null) throw new Exception("Certificado sem chave privada encontrado no .pfx");

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, pfxSenha.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        // Parse do XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlNfe.getBytes(StandardCharsets.UTF_8)));

        // Obtem o Id do infNFe para referência URI
        NodeList infNFeList = doc.getElementsByTagNameNS("http://www.portalfiscal.inf.br/nfe", "infNFe");
        if (infNFeList.getLength() == 0) throw new Exception("Elemento infNFe não encontrado no XML");
        Element infNFe = (Element) infNFeList.item(0);
        String infNFeId = infNFe.getAttribute("Id");

        // Configura o atributo Id para ser reconhecido como ID
        infNFe.setIdAttribute("Id", true);

        // XMLSignatureFactory
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Referência ao elemento infNFe com canonicalização e digest SHA-1
        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform("http://www.w3.org/TR/2001/REC-xml-c14n-20010315",
                (C14NMethodParameterSpec) null));
        Reference ref = fac.newReference(
                "#" + infNFeId,
                fac.newDigestMethod(DigestMethod.SHA1, null),
                transforms,
                null,
                null
        );

        // SignedInfo com RSA-SHA1
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref)
        );

        // KeyInfo com certificado X.509
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        // Assina — insere a assinatura dentro de NFe (após infNFe)
        XMLSignature signature = fac.newXMLSignature(si, ki);
        DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement());
        dsc.setDefaultNamespacePrefix("");
        signature.sign(dsc);

        // Serializa de volta para String
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }
}
