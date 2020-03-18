import java.nio.file.Files
import java.nio.file.Paths

data class Point(val latitude: Float, val longitude: Float)
data class Points(val passengers: Collection<Point>, val drivers: Collection<Point>)

fun main() {
    val points = readPoints()
    for (passenger in points.passengers) {
        val suggested = suggestDrivers(passenger, points.drivers)
        for (point in suggested) {
            println("${point.latitude}, ${point.longitude}")
        }
    }
}

fun suggestDrivers(passengerPoint: Point, driverPoints: Collection<Point>): Collection<Point> {
    TODO("Implement me")
}

private fun readPoints(): Points {
    val pathToResource = Paths.get(Point::class.java.getResource("latlons").toURI())
    val allPoints = Files.readAllLines(pathToResource).map { asPoint(it) }.shuffled()
    val passengers = allPoints.slice(0..9)
    val drivers = allPoints.slice(10..19)
    return Points(passengers, drivers)
}

fun asPoint(it: String): Point {
    val (lat, lon) = it.split(", ")
    return Point(lat.toFloat(), lon.toFloat())
}
