import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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

    objects: car, parkingLot, ParkingLevel, ParkingGarage, ticket,

    Services:
    1. payment service
    2. allocation service --done
    3. exit service --done
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
    List<ParkingSpot> searchByDuration(int level, int duration);

    ParkingSpot searchEmptySpot(VehicleType vehicleType,List<VehicleType> upgrades);
}

interface ITicketService{
    Ticket issueTicket(Vehicle vehicle);
    int exit(Vehicle vehicle);
}

interface IAllocationService{
    Ticket allocate(Vehicle vehicle);
    int deallocate(Vehicle vehicle);
}
abstract class AllocationService implements IAllocationService{
    IParkingSpotSearchService spotSearchService;
    List<Ticket> allocatedTickets;
    CostService costService;
    VehicleType vehicleType;
    List<VehicleType> upgrades;
    AllocationService(VehicleType vehicleType, List<VehicleType> upgrades){
        this.vehicleType = vehicleType;
        this.upgrades = upgrades;
    }
    public Ticket allocate(Vehicle vehicle){
        ParkingSpot spot = spotSearchService.searchEmptySpot(vehicleType,upgrades);
        if(spot!=null && spot.isFree) {
            Ticket ticket = new Ticket(vehicle, spot, LocalDateTime.now());
            vehicle.setTicket(ticket);
            allocatedTickets.add(ticket);
        }
        return null;
    }
    public int deallocate(Vehicle vehicle){
        Ticket ticket = vehicle.ticket;
        ParkingSpot spot = ticket.parkingSpot;
        spot.parkingLevel.addFreeSpot(vehicleType, spot);
        vehicle.ticket = null;
        allocatedTickets.remove(ticket);
        int charge = 0;
        charge = costService.getCharge(ticket.inTime, ticket.outTime);
        return charge;
    }
}
class CarAllocationService extends AllocationService{
    CarAllocationService(List<VehicleType> upgrades){
        super(VehicleType.CAR, upgrades);
    }
}
class ParkingSpotService implements IParkingSpotSearchService{
    List<ParkingLevel> levels;
    @Override
    public List<ParkingSpot> searchByLevel(int level) {
        if(levels.size()>level)
            return levels.get(level).getFreeParkingSpots();

        return null;
    }

    @Override
    public List<ParkingSpot> searchByDuration(int level, int duration) {
        if(levels.size()>level) {
            List<ParkingSpot> list = levels.get(level).getBookedParkingSpots();
            return list.stream().filter(parkingSpot -> LocalDateTime.now().minus(parkingSpot.vehicle.ticket.inTime) >duration).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public ParkingSpot searchEmptySpot(VehicleType vehicleType,List<VehicleType> upgrades) {

        for(ParkingLevel level: levels){
            if(level.hasNextParkingSpot(vehicleType)){
                return level.nextFreeParkingSpot(vehicleType);
            }
        }
        for(VehicleType upgrade: upgrades){
            for(ParkingLevel level: levels){
                if(level.hasNextParkingSpot(upgrade)){
                    return level.nextFreeParkingSpot(upgrade);
                }
            }
        }
        return null;
    }
}
//
enum VehicleType{
    CAR,
    MOTORCYCLE,
    CYCLE,
}
abstract class Vehicle {
    String registrationNumber;
    Color color;
    PersonInfo driver;

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    Ticket ticket;
    VehicleType vehicleType;
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

    public void assign(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    Vehicle vehicle;

    public void setFree(boolean free) {
        isFree = free;
    }

    boolean isFree;
    VehicleType vehicleType;
    ParkingSpot(VehicleType vehicleType){
        this.vehicleType = vehicleType;
    }

}
class CarParkingSpot extends ParkingSpot{
    CarParkingSpot() {
        super(VehicleType.CAR);
    }
}

class MotorCycleParkingSpot extends ParkingSpot {
    MotorCycleParkingSpot() {
        super(VehicleType.MOTORCYCLE);
    }
}


class CycleParkingSpot extends ParkingSpot{
    CycleParkingSpot() {
        super(VehicleType.CYCLE);
    }
}
enum DurationSlab{
    LESS_30,
    LESS_HOUR,
    LESS_2_HOUR,
    MORE_THAN_2_HOUR,
}
class CostService {
    HashMap<DurationSlab,Integer> slab;
    void setCarCost(DurationSlab durationSlab, int cost){
        slab.put(durationSlab, cost);
    }

    public int getCharge(LocalDateTime inTime, LocalDateTime outTime) {
        int duration = outTime.getHour()- inTime.getHour();
        if(duration<30)
            return slab.get(DurationSlab.LESS_30);

        if(duration<60)
            return slab.get(DurationSlab.LESS_HOUR);

        if(duration<120)
            return slab.get(DurationSlab.LESS_2_HOUR);
        return slab.get(DurationSlab.MORE_THAN_2_HOUR);
    }
}
class CarCostService extends CostService {
    //car make more deatiled implementation for chage calculation for each vehicle type if extended.
}
class Ticket{
    Vehicle vehicle;
    ParkingSpot parkingSpot;
    LocalDateTime inTime;
    LocalDateTime outTime;

    public Ticket(Vehicle vehicle, ParkingSpot parkingSpot, LocalDateTime inTime) {
        this.vehicle = vehicle;
        this.parkingSpot = parkingSpot;
        this.inTime = inTime;
        parkingSpot.assign(vehicle);
    }
}
class Loc{
    int x;
    int y;
}

class Entrance {
    Loc loc;
}
class ParkingLevel{
    int level;
    HashMap<VehicleType, ArrayDeque<ParkingSpot>> availSpots;
    HashMap<VehicleType, ArrayDeque<ParkingSpot>> bookedSpots;
    int maxSpots;
    int spots;
    List<Entrance> entrances;

    boolean hasNextParkingSpot(VehicleType type){
        return !availSpots.get(type).isEmpty();
    }
    List<ParkingSpot> getFreeParkingSpots(){
        List<ParkingSpot> list = new ArrayList<>();
        for(ArrayDeque<ParkingSpot> ar: availSpots.values())
            list.addAll(ar);
        return list;
    }

    List<ParkingSpot> getBookedParkingSpots(){
        List<ParkingSpot> list = new ArrayList<>();
        for(ArrayDeque<ParkingSpot> ar: bookedSpots.values())
            list.addAll(ar);
        return list;
    }

    ParkingSpot nextFreeParkingSpot(VehicleType vehicleType){
        if(!hasNextParkingSpot(vehicleType)) return null;
        ParkingSpot spot = availSpots.get(vehicleType).getFirst();
        return spot;
    }
    ParkingSpot bookSpot(ParkingSpot spot, VehicleType type){
        if(availSpots.get(type).remove(spot)) {
            bookedSpots.get(type).add(spot);
            return spot;
        }
        //failed to book
        return null;
    }
    void addFreeSpot(VehicleType type, ParkingSpot spot){
        availSpots.get(type).add(spot);
        spot.setFree(true);
    }
    void addParkingSpot(VehicleType type){
        if(type==VehicleType.CAR)
            availSpots.get(type).add(new CarParkingSpot());
        if(type==VehicleType.MOTORCYCLE)
            availSpots.get(type).add(new MotorCycleParkingSpot());
        if(type==VehicleType.CYCLE)
            availSpots.get(type).add(new CycleParkingSpot());
    }
}
class ParkingLot{
    List<ParkingLevel> parkingLevels;
    HashMap<VehicleType, Integer> maxSpotsVehicleType;
    HashMap<ParkingLevel,List<Entrance>> entrances;
    List<Ticket> tickets;
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

/*class VehicleAllocatorSerive{
    //    VehicleParkAllocator cycleAl;
//    VehicleParkAllocator motorcycleAl;
//    VehicleParkAllocator carAl;
////intialize logic for all vehicleAllocators
//    public VehicleParkAllocator getAllocator(Vehicle vehicle) {
//        if(vehicle.vehicleType== VehicleType.CAR)
//                return carAl;
//        if(vehicle.vehicleType== VehicleType.MOTORCYCLE)
//            return motorcycleAl;
//        if(vehicle.vehicleType== VehicleType.CYCLE)
//            return cycleAl;
//    }
//}
abstract class VehicleParkAllocator {
    List<VehicleType> upgrades;
    List<ParkingLevel> parkingLevels;

    abstract void setUpgrades();

    ParkingSpot allocateParkingSpot(VehicleType vehicletype){
        ParkingSpot spot=null;
        for(ParkingLevel level: parkingLevels){
            if(level.hasNextParkingSpot(vehicletype)) {
                spot = level.nextFreeParkingSpot(vehicletype);
                level.bookSpot(spot, vehicletype);
                return spot;
            }
        }
        return null;
    }
}
class CarParkSlotAllocater extends VehicleParkAllocator {
    static CarParkSlotAllocater carParkSlotAllocater;
    public static VehicleParkAllocator getInstance() {
        if(carParkSlotAllocater!=null) return carParkSlotAllocater;
        carParkSlotAllocater = new CarParkSlotAllocater();
        return carParkSlotAllocater;
    }

    @Override
    void setUpgrades() {
        upgrades.add(VehicleType.MOTORCYCLE);
        upgrades.add(VehicleType.CYCLE);
    }

}

class LargeVehicleParkSlotAllocator extends VehicleParkAllocator{
    static LargeVehicleParkSlotAllocator largeVehicleParkSlotAllocator;
    public static VehicleParkAllocator getInstance() {
        if(largeVehicleParkSlotAllocator !=null) return largeVehicleParkSlotAllocator;
        largeVehicleParkSlotAllocator = new LargeVehicleParkSlotAllocator();
        return largeVehicleParkSlotAllocator;
    }
    private LargeVehicleParkSlotAllocator(){

    }
    @Override
    void setUpgrades() {
        upgrades.add(VehicleType.CYCLE);
    }
}

class CycleParkAllocator extends VehicleParkAllocator{
    static CycleParkAllocator cycleParkAllocator;
    public static VehicleParkAllocator getInstance() {
        if(cycleParkAllocator!=null) return cycleParkAllocator;
        cycleParkAllocator = new CycleParkAllocator();
        return cycleParkAllocator;
    }
    private CycleParkAllocator(){

    }
    @Override
    void setUpgrades() {
        upgrades.add(VehicleType.MOTORCYCLE);
    }

}
*/
