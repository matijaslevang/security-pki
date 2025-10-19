export interface CreateCertificate {
    issuerSerialNumber: string,
    selfSigned: boolean,
    intermediate: boolean,
    skiaki: boolean,
    sanString: string,
    startDate: Date,
    endDate: Date,
    subjectDto: Subject,
    keyUsageValues: boolean[],
    extKeyUsageValues: boolean[],
}
 
export interface Subject {
    commonName: string,
    surname: string,
    givenName: string,
    organization: string,
    department: string,
    email: string,
    country: string
}

export interface CertificateInfo {
  serialNumber: string;
  subject: string;
  issuer: string;
  validFrom: string;
  validTo: string;
  status: CertificateStatus;
  isCa: boolean;
}

export interface CertificateChain {
  rootCertificate: CertificateInfo;
  issuedCertificates: CertificateInfo[];
}

export interface CertificateRow {
  certificate: CertificateInfo;
  depth: number;
}

export interface CertificateChainDisplay {
  chainRows: CertificateRow[];
}

export interface IssuingCertificate {
  serialNumber: string;
  subject: string;
}

export enum CertificateStatus {
    VALID = 'VALID',
    REVOKED = 'REVOKED',
    EXPIRED = 'EXPIRED'
}

export interface CreateCertTemplate {
  serialNumber: string,
  commonNameRegex: string,
  sanRegex: string,
  ttl: number,
  skiakiDefaultValue: boolean,
  keyUsageDefaultValues: boolean[],
  extKeyUsageDefaultValues: boolean[],
}

export interface CertTemplate {
  id: number,
  serialNumber: string,
  commonNameRegex: string,
  sanRegex: string,
  ttl: number,
  skiakiDefaultValue: boolean,
  keyUsageDefaultValues: boolean[],
  extKeyUsageDefaultValues: boolean[],
} 