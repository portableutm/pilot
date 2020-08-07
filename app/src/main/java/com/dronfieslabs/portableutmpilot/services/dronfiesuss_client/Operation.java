package com.dronfieslabs.portableutmpilot.services.dronfiesuss_client;

import java.util.List;

class Operation {

    private String name;
    private String flight_comments;
    private String volumes_description;
    private String flight_number;
    private String submit_time;
    private String update_time;
    private int faa_rule;
    private int state;
    private boolean near_structure;
    private String contact;
    private String aircraft_comments;
    private List<String> uas_registrations;
    private PriorityElements priority_elements;
    private List<ContingencyPlan> contingency_plans;
    private List<OperationVolume> operation_volumes;
    private List<NegotiationAgreement> negotiation_agreements;

    public Operation(String name, String flight_comments, String volumes_description, String flight_number,
                     String submit_time, String update_time, int faa_rule, int state, boolean near_structure,
                     String contact, String aircraft_comments, List<String> uas_registrations, PriorityElements priority_elements,
                     List<ContingencyPlan> contingency_plans, List<OperationVolume> operation_volumes,
                     List<NegotiationAgreement> negotiation_agreements) {
        this.name = name;
        this.flight_comments = flight_comments;
        this.volumes_description = volumes_description;
        this.flight_number = flight_number;
        this.submit_time = submit_time;
        this.update_time = update_time;
        this.faa_rule = faa_rule;
        this.state = state;
        this.near_structure = near_structure;
        this.contact = contact;
        this.aircraft_comments = aircraft_comments;
        this.uas_registrations = uas_registrations;
        this.priority_elements = priority_elements;
        this.contingency_plans = contingency_plans;
        this.operation_volumes = operation_volumes;
        this.negotiation_agreements = negotiation_agreements;
    }
}
