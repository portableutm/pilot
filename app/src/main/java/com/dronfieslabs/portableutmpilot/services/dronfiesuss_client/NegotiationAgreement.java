package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

class NegotiationAgreement {
    private String free_text;
    private String discovery_reference;
    private String type;
    private String uss_name;
    private String uss_name_of_originator;
    private String uss_name_of_receiver;

    public NegotiationAgreement(String free_text, String discovery_reference, String type, String uss_name, String uss_name_of_originator, String uss_name_of_receiver) {
        this.free_text = free_text;
        this.discovery_reference = discovery_reference;
        this.type = type;
        this.uss_name = uss_name;
        this.uss_name_of_originator = uss_name_of_originator;
        this.uss_name_of_receiver = uss_name_of_receiver;
    }
}
