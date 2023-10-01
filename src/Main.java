import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @ParkingLot
 * Requriments:
 *     parking lots are numbered 1-n with 1 being nearer to the entry
 * 1. Allocate a parking lot to the car if it's available, always allocate nearer spot to the entry
 * 2. For allocation maintain details like color, registration number,
 * 3. Free up the spot upon return of the ticket by the driver
 * 4. Registration numbers of all cars of a particular color.
 * 5. Slot number in which a car with a given registration number is parked.
 * 6. Slot numbers of all slots where a car of a particular color is parked.
 * 7. charge the user for parking service
 * 8. track entry and exit time at parking slot

posibilities:
1. can have multiple exit points
2. can have multiple ways to pay
3. not all slots are suitable for all types of car (type specific car slots availibility tracking)
4. multilevel parking
5. Reservation for handicapped parking for all types of car types

problems:
1. For multiple exits and entrances numbering of nearer spots
2. maintaining different types of parking spots availibity for drivers and cars specific.

 **/
/*

    objects: car, parkingLot, ParkingLevel, ParkingGarage ticket

    Services:
    1. payment service
    2. allocation service --done
    3. exit service
    4. numbering service
    5. search service
        --  by color
        --  by registration number
        --  by car type
        --  by duration
        --  by cost
        --  by parking lot car type location
        --  by drive type location
    6.
 */
interface IVehicleSearchService{
    List<Vehicle> searchByColor(String color);
    List<Vehicle> searchByRegistrationNumber(String number);
    List<Vehicle> searchByCarType(String carType);
    List<Vehicle> searchByDuration(int duration);
}

interface IParkingSpotSearchService{
    List<ParkingSpot> searchByLevel(int level);
    List<ParkingSpot> searchByDuration(int level);
}

interface ITicketService{
    Ticket issueTicket(Vehicle vehicle);
    int exit(Vehicle vehicle);
}
class TicketService implements ITicketService{
    VehicleParkAllocator vehicleParkAllocator;
    VehicleAllocatorSerive allocatorSerive;
    List<Ticket> issuedTickets;

    public List<Ticket> getIssuedTickets() {
        return issuedTickets;
    }

    @Override
    public Ticket issueTicket(Vehicle vehicle){
        vehicleParkAllocator = allocatorSerive.getAllocator(vehicle);
        Ticket ticket = vehicleParkAllocator.allocateParkingSpot(vehicle);
        issuedTickets.add(ticket);
        return ticket;
    }
    int chargeCalculator(Ticket ticket){
        Vehicle vehicle = ticket.vehicle;
        VehicleParkAllocator vehicleParkAllocator;
        int duration = 100;
        int charge = ticket.parkingSpot.getCharge();
        issuedTickets.remove(ticket);
        vehicleParkAllocator = allocatorSerive.getAllocator(vehicle);
        vehicleParkAllocator.deallocate(ticket.parkingSpot);
        return charge*duration;
    }

    @Override
    public int exit(Vehicle vehicle) {
        ParkingSpot parkingSpot = vehicle.ticket.parkingSpot;
        VehicleParkAllocator vehicleParkAllocator;
        vehicleParkAllocator = allocatorSerive.getAllocator(vehicle);
        vehicleParkAllocator.deallocate(parkingSpot);
        int charge = chargeCalculator(vehicle.ticket);
        return charge;
    }
}
class VehicleAllocatorSerive{
    VehicleParkAllocator motorcycleGAllocater;
    VehicleParkAllocator motorcycleSAllocater;
    VehicleParkAllocator cycleGAllocater;
    VehicleParkAllocator cycleSAllocater;
    VehicleParkAllocator carGAllocater;
    VehicleParkAllocator carSAllocater;

    public VehicleParkAllocator getAllocator(Vehicle vehicle) {
        if(vehicle instanceof Car && vehicle.driver.driverType instanceof General)
            return carGAllocater;
        if(vehicle instanceof Car && vehicle.driver.driverType instanceof SpecialAbled)
            return carSAllocater;
        if(vehicle instanceof Motorcycle && vehicle.driver.driverType instanceof General)
            return motorcycleGAllocater;
        if(vehicle instanceof Motorcycle && vehicle.driver.driverType instanceof SpecialAbled)
            return motorcycleSAllocater;
        if(vehicle instanceof Cycle && vehicle.driver.driverType instanceof General)
            return cycleGAllocater;
        if(vehicle instanceof Cycle && vehicle.driver.driverType instanceof SpecialAbled)
            return cycleSAllocater;
        return null;
    }
}
abstract class Vehicle {
    String registrationNumber;
    Color color;
    PersonInfo driver;
    Ticket ticket;
}
class Car extends Vehicle{

}
class Motorcycle extends Vehicle{

}
class Cycle extends Vehicle{

}
abstract class ParkingSpot{
    int spotNumber;
    ParkingLevel parkingLevel;
    Loc loc;
    int duration;
    int spots;
    List<Integer> costSlab;
    int getCharge(){
        int duration=100;
        //slab logic based on duration
        int slabIdx=0;
        return costSlab.get(slabIdx)*duration;
    }
}
abstract class CarParkingSpot extends ParkingSpot{
   // int charge;
    int maxTime;
    int maxSpots;

}
class CarGeneralSpot extends CarParkingSpot{

}

class CarSpeciallyAbledSpot extends CarParkingSpot{

}
abstract class MotorcycleParkingSpot extends ParkingSpot implements DriverType{
    int charge;
    int maxTime;
}

class MotorcycleGeneralSpot extends MotorcycleParkingSpot{

}

class MotorcycleSpeciallyAbledSpot extends MotorcycleParkingSpot{

}
class CycleParkingSpot extends ParkingSpot implements DriverType{
    int charge;
    int maxTime;
}
class CycleGeneralSpot extends CycleParkingSpot{

}

class CycleSpeciallyAbledSpot extends CycleParkingSpot{

}
abstract class VehicleParkAllocator {
    List<VehicleParkAllocator> upgrades;
    List<ParkingSpot> parkingSpots;
    Ticket allocateParkingSpot(Vehicle vehicle){
        if(parkingSpots.size()!=0){
            Ticket ticket = new Ticket();
            ticket.vehicle = vehicle;
            ticket.parkingSpot = parkingSpots.remove(0);
            ticket.inTime = LocalDateTime.now();
            vehicle.ticket = ticket;
            return ticket;
        }
        for(VehicleParkAllocator diffSpotAllocator: upgrades){
            Ticket ticket = diffSpotAllocator.allocateParkingSpot(vehicle);
            if(ticket!=null) {
                vehicle.ticket = ticket;
                return ticket;
            }
        }
        return null;
    }

    abstract void addParkingSlot();
    abstract void setUpgrades();

    public abstract void deallocate(ParkingSpot parkingSpot);
}
abstract class CarParkSlotAllocater extends VehicleParkAllocator {
    @Override
    void setUpgrades() {
        upgrades.add(MotorcycleParkGeneralSlotAllocator.getInstance());
        upgrades.add(MotorcycleParkSpecialSlotAllocator.getInstance());
        upgrades.add(CycleParkGeneralSlotAllocator.getInstance());
        upgrades.add(CycleParkSpecialSlotAllocator.getInstance());
    }
}

class CarParkGeneralSlotAllocator extends CarParkSlotAllocater{
    static CarParkGeneralSlotAllocator carParkGeneralSlotAllocator;
    private CarParkGeneralSlotAllocator() {

    }
    public static CarParkGeneralSlotAllocator getInstance(){
        if(carParkGeneralSlotAllocator==null){
            carParkGeneralSlotAllocator =  new CarParkGeneralSlotAllocator();
        }
        return carParkGeneralSlotAllocator;
    }

    void addParkingSlot(){
        ParkingSpot parkingSpot = new CarGeneralSpot();
        parkingSpots.add(parkingSpot);
    }
}

class CarParkSpecialSlotAllocator extends CarParkSlotAllocater{

    static CarParkSpecialSlotAllocator carParkSpecialSlotAllocator;
    private CarParkSpecialSlotAllocator() {

    }
    public static CarParkSpecialSlotAllocator getInstance(){
        if(carParkSpecialSlotAllocator==null){
            carParkSpecialSlotAllocator =  new CarParkSpecialSlotAllocator();
        }
        return carParkSpecialSlotAllocator;
    }

    void addParkingSlot(){
        ParkingSpot parkingSpot = new CarSpeciallyAbledSpot();
        parkingSpots.add(parkingSpot);
    }
}

abstract class MotorcycleParkSlotAllocater extends VehicleParkAllocator{
    void addParkingSlot(){
        ParkingSpot parkingSpot = new CarGeneralSpot();
        parkingSpots.add(parkingSpot);
    }
    void setUpgrades() {
        upgrades.add(CycleParkGeneralSlotAllocator.getInstance());
        upgrades.add(CycleParkSpecialSlotAllocator.getInstance());
    }
}

class MotorcycleParkGeneralSlotAllocator extends CarParkSlotAllocater{
    static MotorcycleParkGeneralSlotAllocator motorcycleParkGeneralSlotAllocator;
    private MotorcycleParkGeneralSlotAllocator() {

    }
    public static MotorcycleParkGeneralSlotAllocator getInstance(){
        if(motorcycleParkGeneralSlotAllocator ==null){
            motorcycleParkGeneralSlotAllocator =  new MotorcycleParkGeneralSlotAllocator();
        }
        return motorcycleParkGeneralSlotAllocator;
    }
    void addParkingSlot(){
        ParkingSpot parkingSpot = new MotorcycleGeneralSpot();
        parkingSpots.add(parkingSpot);
    }
}

class MotorcycleParkSpecialSlotAllocator extends CarParkSlotAllocater {
    static MotorcycleParkSpecialSlotAllocator motorcycleParkSpecialSlotAllocator;
    private MotorcycleParkSpecialSlotAllocator() {

    }
    public static MotorcycleParkSpecialSlotAllocator getInstance(){
        if(motorcycleParkSpecialSlotAllocator ==null){
            motorcycleParkSpecialSlotAllocator =  new MotorcycleParkSpecialSlotAllocator();
        }
        return motorcycleParkSpecialSlotAllocator;
    }
    void addParkingSlot(){
        ParkingSpot parkingSpot = new MotorcycleSpeciallyAbledSpot();
        parkingSpots.add(parkingSpot);
    }
}
abstract class CycleSlotAllocater extends VehicleParkAllocator{

}

class CycleParkGeneralSlotAllocator extends CarParkSlotAllocater{
    static CycleParkGeneralSlotAllocator cycleParkGeneralSlotAllocator;
    private CycleParkGeneralSlotAllocator() {

    }
    public static CycleParkGeneralSlotAllocator getInstance(){
        if(cycleParkGeneralSlotAllocator ==null){
            cycleParkGeneralSlotAllocator =  new CycleParkGeneralSlotAllocator();
        }
        return cycleParkGeneralSlotAllocator;
    }
    void addParkingSlot(){
        ParkingSpot parkingSpot = new CycleGeneralSpot();
        parkingSpots.add(parkingSpot);
    }
    @Override
    void setUpgrades() {
        this.upgrades = null;
    }
}

class CycleParkSpecialSlotAllocator extends CarParkSlotAllocater {
    static CycleParkSpecialSlotAllocator cycleParkSpecialSlotAllocator;
    private CycleParkSpecialSlotAllocator() {

    }
    public static CycleParkSpecialSlotAllocator getInstance(){
        if(cycleParkSpecialSlotAllocator==null){
            cycleParkSpecialSlotAllocator =  new CycleParkSpecialSlotAllocator();
        }
        return cycleParkSpecialSlotAllocator;
    }

    void addParkingSlot(){
        ParkingSpot parkingSpot = new CycleSpeciallyAbledSpot();
        parkingSpots.add(parkingSpot);
    }
}
interface DriverType{

}
class General implements DriverType{

}
class SpecialAbled implements DriverType{

}
class Ticket{
    Vehicle vehicle;
    ParkingSpot parkingSpot;
    LocalDateTime inTime;
    LocalDateTime outTime;


}
class Loc{
    int x;
    int y;
}

class Entrance extends Loc{

}
class ParkingLevel{
    int level;
    List<ParkingSpot> availParkingSpots;
    //List<ParkingSpot> bookedParkingSpots;
    List<Loc> entrances;
}


enum DType{
    GENERAL,
    SPECIALLYABLED,
}

enum Color{
   BLUE,
   BLACK,
   WHITE,
   RED,
   GREY,
     YELLOW,
     GREEN,
 }
class PersonInfo{
    private String uuid;
    Name name;
    private Date dateOfBirth;
    private Address permanentAddress;
    private Address temproaryAddress;
    private List<Card> listOfCards;
    private List<IContactInfo> contactInformation;
    DriverType driverType;
}
class Name{
    String firstName;
    String middleName;
    String lastName;
}
interface  IContactInfo{
   public String getContactInfo();
}
class EmailAddress implements IContactInfo{
    String userName;
    String domainName;

    @Override
    public String getContactInfo() {
        return userName+"@"+domainName;
    }
}
class ContactNumber implements IContactInfo{
    String contactNumber;
    int countryCode;
    @Override
    public String getContactInfo() {
        return "+"+countryCode+contactNumber;
    }
}
class Card{
    UUID cardNumber;
    CardType cardType;
}
enum CardType{
    CREDITCARD,
    DEBITCARD,
    DRIVERLICENCE,
    SSN,
    EADCARD,
}
class Address{
    String stNumber1;
    String stNumber2;
    Country country;
    County county;
    State state;
    int zipcode;
}
enum Country{

}
enum County{

}
enum State{

}

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}