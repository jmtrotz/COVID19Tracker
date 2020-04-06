package com.jefftrotz.covid19tracker;

/**
 * Simple class to convert a full, spelled out US state name to its two letter abbreviation
 * or to its corresponding position (AKA index number) in the spinner/drop down list
 * @author Jeffrey Trotz
 * @date 4/1/2020
 * @version 1.0
 */
public class StateNameConverter
{
    /**
     * Converts the state name retrieved from location data to its 2 letter
     * abbreviation as required by the API
     * @param stateName Full state name (as a String) from location data or spinner
     * @return Returns the 2 letter abbreviation of the state as a String
     */
    public String getStateAbbreviation(String stateName)
    {
        switch (stateName)
        {
            case "Alabama":
                return "AL";
            case "Alaska":
                return "AK";
            case "Arizona":
                return "AZ";
            case "Arkansas":
                return "AR";
            case "California":
                return "CA";
            case "Colorado":
                return "CO";
            case "Connecticut":
                return "CT";
            case "Delaware":
                return "DE";
            case "Florida":
                return "FL";
            case "Georgia":
                return "GA";
            case "Hawaii":
                return "HI";
            case "Idaho":
                return "ID";
            case "Illinois":
                return "IL";
            case "Indiana":
                return "IN";
            case "Iowa":
                return "IA";
            case "Kansas":
                return "KS";
            case "Kentucky":
                return "KY";
            case "Louisiana":
                return "LA";
            case "Maine":
                return "ME";
            case "Maryland":
                return "MD";
            case "Massachusetts":
                return "MA";
            case "Michigan":
                return "MI";
            case "Minnesota":
                return "MN";
            case "Mississippi":
                return "MS";
            case "Missouri":
                return "MO";
            case "Montana":
                return "MT";
            case "Nebraska":
                return "NE";
            case "Nevada":
                return "NV";
            case "New Hampshire":
                return "NH";
            case "New Jersey":
                return "NJ";
            case "New Mexico":
                return "NM";
            case "New York":
                return "NY";
            case "North Carolina":
                return "NC";
            case "North Dakota":
                return "ND";
            case "Ohio":
                return "OH";
            case "Oklahoma":
                return "OK";
            case "Oregon":
                return "OR";
            case "Pennsylvania":
                return "PA";
            case "Rhode Island":
                return "RI";
            case "South Carolina":
                return "SC";
            case "South Dakota":
                return "SD";
            case "Tennessee":
                return "TN";
            case "Texas":
                return "TX";
            case "Utah":
                return "UT";
            case "Vermont":
                return "VT";
            case "Virginia":
                return "VA";
            case "Washington":
                return "WA";
            case "Washington DC":
                return "DC";
            case "West Virginia":
                return "WV";
            case "Wisconsin":
                return "WI";
            case "Wyoming":
                return "WY";
        }

        return null;
    }

    /**
     * Converts the state name retrieved from location data to its corresponding position
     * (AKA index number) in the spinner/drop down list
     * @param stateName Full state name (as a String) from location data or spinner
     * @return Returns the index position as an int
     */
    public int getStateSpinnerIndex(String stateName)
    {
        switch (stateName)
        {
            case "Alabama":
                return 0;
            case "Alaska":
                return 1;
            case "Arizona":
                return 2;
            case "Arkansas":
                return 3;
            case "California":
                return 4;
            case "Colorado":
                return 5;
            case "Connecticut":
                return 6;
            case "Delaware":
                return 7;
            case "Florida":
                return 8;
            case "Georgia":
                return 9;
            case "Hawaii":
                return 10;
            case "Idaho":
                return 11;
            case "Illinois":
                return 12;
            case "Indiana":
                return 13;
            case "Iowa":
                return 14;
            case "Kansas":
                return 15;
            case "Kentucky":
                return 16;
            case "Louisiana":
                return 17;
            case "Maine":
                return 18;
            case "Maryland":
                return 19;
            case "Massachusetts":
                return 20;
            case "Michigan":
                return 21;
            case "Minnesota":
                return 22;
            case "Mississippi":
                return 23;
            case "Missouri":
                return 24;
            case "Montana":
                return 25;
            case "Nebraska":
                return 26;
            case "Nevada":
                return 27;
            case "New Hampshire":
                return 28;
            case "New Jersey":
                return 29;
            case "New Mexico":
                return 30;
            case "New York":
                return 31;
            case "North Carolina":
                return 32;
            case "North Dakota":
                return 33;
            case "Ohio":
                return 34;
            case "Oklahoma":
                return 35;
            case "Oregon":
                return 36;
            case "Pennsylvania":
                return 37;
            case "Rhode Island":
                return 38;
            case "South Carolina":
                return 39;
            case "South Dakota":
                return 40;
            case "Tennessee":
                return 41;
            case "Texas":
                return 42;
            case "Utah":
                return 43;
            case "Vermont":
                return 44;
            case "Virginia":
                return 45;
            case "Washington":
                return 46;
            case "Washington DC":
                return 47;
            case "West Virginia":
                return 48;
            case "Wisconsin":
                return 49;
            case "Wyoming":
                return 50;
        }

        return 0;
    }
}
