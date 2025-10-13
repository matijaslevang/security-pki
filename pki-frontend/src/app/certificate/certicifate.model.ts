export interface CreateCertificate {
    issuerUuid: string,
    selfSigned: boolean,
    intermediate: boolean,
    skiaki: boolean,
    sanString: string,
    startDate: Date,
    endDate: Date,
    subjectDto: Subject
}
 
export interface Subject {
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

export enum CertificateStatus{
    VALID,
    REVOKED,
    EXPIRED
}