package com.quantasip.plotpoint;

import java.util.HashMap;

// This class is used to store and manage form data locally.
public class FormData {
    private static final HashMap<String, Form> formDatabase = new HashMap<>();

    // Add a form entry to the database using Aadhar number as key
    public static void addForm(String aadharNumber, String fullName, String dob) {
        formDatabase.put(aadharNumber, new Form(aadharNumber, fullName, dob));
    }

    // Retrieve form data using Aadhar number
    public static Form getForm(String aadharNumber) {
        return formDatabase.get(aadharNumber);
    }

    // Inner class to represent the form data
    public static class Form {
        private final String aadharNumber;
        private final String fullName;
        private final String dob;

        // Constructor to initialize form
        public Form(String aadharNumber, String fullName, String dob) {
            this.aadharNumber = aadharNumber;
            this.fullName = fullName;
            this.dob = dob;
        }

        // Getters for the form attributes
        public String getAadharNumber() {
            return aadharNumber;
        }

        public String getFullName() {
            return fullName;
        }

        public String getDob() {
            return dob;
        }
    }
}
