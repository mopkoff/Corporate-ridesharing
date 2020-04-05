import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.*


data class Point(val latitude: Float, val longitude: Float)
data class Participants(val passengers: Collection<Person>, val drivers: Collection<Person>)
data class Person(val id: UUID, val finishPoint: Point)

//JB coords
val DEPARTURE_POINT = Point(59.981557F, 30.214507F)

const val MAX_ACCUMULATED_DIFFERENCE_IN_DEG = 1  //max allowed error

//for orthodromic distance
const val EARTH_RADIUS = 6371.007177356707  // kmeters

//for Euclid distance, coeffs for SPB
const val LAT_COEF = 111.412 // kmeters
const val LNG_COEF = 55.800  //kmeters

fun main() {
    val (passengers, drivers) = readPoints()
    for (passenger in passengers) {
        val suggestedDrivers = suggestDrivers(passenger, drivers)
        println("Passenger point: ${passenger.finishPoint.latitude}, ${passenger.finishPoint.longitude}")
        for (driver in suggestedDrivers) {
            println("  ${driver.finishPoint.latitude}, ${driver.finishPoint.longitude}")
        }
    }
}

//sort by ascending measure
fun suggestDrivers(passenger: Person, drivers: Collection<Person>): Collection<Person> {
    return drivers.sortedWith(compareBy { calculateMeasure(passenger.finishPoint, it.finishPoint) });
}

private fun readPoints(): Participants {
    val pathToResource = Paths.get(Point::class.java.getResource("latlons").toURI())
    val allPoints = Files.readAllLines(pathToResource).map { asPoint(it) }.shuffled()
    val passengers = allPoints.slice(0..9).map { Person(UUID.randomUUID(), it) }
    val drivers = allPoints.slice(10..19).map { Person(UUID.randomUUID(), it) }
    return Participants(passengers, drivers)
}

private fun asPoint(it: String): Point {
    val (lat, lon) = it.split(", ")
    return Point(lat.toFloat(), lon.toFloat())
}

/**
 * Measure extra distance which a driver need to drive
 * Less is better
 */
private fun calculateMeasure(passengerDestinationPoint: Point, driverDestinationPoint: Point): Double {
    val isMatrixDistanceApiExist = false;

    if (isMatrixDistanceApiExist)
        return calculateExtraDistance(DEPARTURE_POINT, passengerDestinationPoint, driverDestinationPoint, ::biCustomDistance);
    else {
        //if sum of difference in latitude is more than MAX_ACCUMULATED_DIFFERENCE_IN_DEG, we need to use more accurate algorithm
        if (abs(DEPARTURE_POINT.latitude - passengerDestinationPoint.latitude)
                + abs(passengerDestinationPoint.latitude - driverDestinationPoint.latitude)
                > MAX_ACCUMULATED_DIFFERENCE_IN_DEG)
            return calculateExtraDistance(DEPARTURE_POINT, passengerDestinationPoint, driverDestinationPoint, ::biOrthodromicDistance);
        return calculateExtraDistance(DEPARTURE_POINT, passengerDestinationPoint, driverDestinationPoint, ::biEuclidDistance);
    }

}

/**
 * Extra distance which a driver need to drive
 *
 * @param alg algorithm to calculate distance between points
 */
fun calculateExtraDistance(departurePoint: Point,
                           passengerDestinationPoint: Point,
                           driverDestinationPoint: Point,
                           alg: (departurePoint: Point, destinationPoint: Point) -> Double): Double {
    return alg(departurePoint, passengerDestinationPoint) // distance to deliver a passenger
    + alg(passengerDestinationPoint, driverDestinationPoint) // distance to deliver a driver
    - alg(departurePoint, driverDestinationPoint) // distance to deliver a driver w/o a passenger
}

/**
 * Direct distance calculating.
 * Roads, traffic jams and type of transport are excluded.
 */
fun biOrthodromicDistance(departurePoint: Point, destinationPoint: Point): Double {
    val lat1 = departurePoint.latitude.toDouble();
    val lng1 = departurePoint.longitude.toDouble();
    val lat2 = destinationPoint.latitude.toDouble();
    val lng2 = destinationPoint.longitude.toDouble();

    val latDiff = Math.toRadians(abs(lat2 - lat1))
    val lngDiff = Math.toRadians(abs(lng2 - lng1))
    val a = sin(latDiff / 2) * sin(latDiff / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(lngDiff / 2) * sin(lngDiff / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS * c
}

/**
 * In case we have low scale, we can increase performance comparing to biOrthodromicDistance
 *
 */
fun biEuclidDistance(departurePoint: Point, destinationPoint: Point): Double {
    val lat1 = departurePoint.latitude.toDouble();
    val lng1 = departurePoint.longitude.toDouble();
    val lat2 = destinationPoint.latitude.toDouble();
    val lng2 = destinationPoint.longitude.toDouble();

    return sqrt(((lat1 - lat2) * LAT_COEF).pow(2) + ((lng1 - lng2) * LNG_COEF).pow(2));
}

/**
 * In case we have some api, that calculate distance between points using distance matrix, for example Google or Bing API
 */
fun biCustomDistance(departurePoint: Point, destinationPoint: Point): Double {

    return Math.random() *
            departurePoint.latitude *
            departurePoint.longitude *
            destinationPoint.latitude *
            destinationPoint.longitude;
}
