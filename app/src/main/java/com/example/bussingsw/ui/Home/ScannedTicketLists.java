package com.example.bussingsw.ui.Home;

public class ScannedTicketLists {

    private String TicketFrom, TicketTo, BookingDate, BookingTime, PassengerType, Discount, TicketNo, TicketPrice;

    public ScannedTicketLists(String ticketFrom, String ticketTo, String bookingDate, String bookingTime, String passengerType, String discount, String ticketNo, String ticketPrice) {
        TicketFrom = ticketFrom;
        TicketTo = ticketTo;
        BookingDate = bookingDate;
        BookingTime = bookingTime;
        PassengerType = passengerType;
        Discount = discount;
        TicketNo = ticketNo;
        TicketPrice = ticketPrice;
    }

    public ScannedTicketLists() {
    }

    public String getTicketFrom() {
        return TicketFrom;
    }

    public void setTicketFrom(String ticketFrom) {
        TicketFrom = ticketFrom;
    }

    public String getTicketTo() {
        return TicketTo;
    }

    public void setTicketTo(String ticketTo) {
        TicketTo = ticketTo;
    }

    public String getBookingDate() {
        return BookingDate;
    }

    public void setBookingDate(String bookingDate) {
        BookingDate = bookingDate;
    }

    public String getBookingTime() {
        return BookingTime;
    }

    public void setBookingTime(String bookingTime) {
        BookingTime = bookingTime;
    }

    public String getPassengerType() {
        return PassengerType;
    }

    public void setPassengerType(String passengerType) {
        PassengerType = passengerType;
    }

    public String getDiscount() {
        return Discount;
    }

    public void setDiscount(String discount) {
        Discount = discount;
    }

    public String getTicketNo() {
        return TicketNo;
    }

    public void setTicketNo(String ticketNo) {
        TicketNo = ticketNo;
    }

    public String getTicketPrice() {
        return TicketPrice;
    }

    public void setTicketPrice(String ticketPrice) {
        TicketPrice = ticketPrice;
    }
}
