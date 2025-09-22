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