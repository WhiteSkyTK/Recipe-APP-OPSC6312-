import XCTest
@testable import HarmonyKitchen

final class UnitConverterTests: XCTestCase {
    func testParsesNumbersFractionsAndRanges() {
        XCTAssertEqual(UnitConverter.parseQuantity("2"), 2)
        XCTAssertEqual(UnitConverter.parseQuantity("1/2"), 0.5)
        XCTAssertEqual(UnitConverter.parseQuantity("1 1/2"), 1.5)
        XCTAssertEqual(UnitConverter.parseQuantity("1-2"), 1.5)
        XCTAssertEqual(UnitConverter.parseQuantity("1 to 3"), 2)
        XCTAssertEqual(UnitConverter.parseQuantity(""), 0)
        XCTAssertNil(UnitConverter.parseQuantity("pinch"))
        XCTAssertNil(UnitConverter.parseQuantity("to taste"))
    }

    /// The Android Tasty mapper turned "1½" into "10.5".
    func testStuckUnicodeFractionIsAddedNotConcatenated() {
        XCTAssertEqual(UnitConverter.parseQuantity("1½"), 1.5)
        XCTAssertEqual(UnitConverter.parseQuantity("2¼"), 2.25)
    }

    func testConvertsToMetric() {
        XCTAssertEqual(UnitConverter.convert(quantity: "2", unit: "cups", to: .metric), "473.2 ml")
        XCTAssertEqual(UnitConverter.convert(quantity: "1500", unit: "g", to: .metric), "1.5 kg")
    }

    func testConvertsToImperial() {
        XCTAssertEqual(UnitConverter.convert(quantity: "500", unit: "g", to: .imperial), "1.1 lbs")
        XCTAssertEqual(UnitConverter.convert(quantity: "1", unit: "tsp", to: .imperial), "1 tsp")
        XCTAssertEqual(UnitConverter.convert(quantity: "1", unit: "tbsp", to: .imperial), "1 tbsp")
        XCTAssertEqual(UnitConverter.convert(quantity: "2", unit: "cups", to: .imperial), "2 cups")
    }

    /// Exactly one cup used to come out as "16 tbsp" because of rounding in the constants.
    func testOneCupStaysOneCup() {
        XCTAssertEqual(UnitConverter.convert(quantity: "1", unit: "cup", to: .imperial), "1 cup")
    }

    func testTextQuantitiesAreLeftAlone() {
        XCTAssertEqual(UnitConverter.convert(quantity: "a pinch", unit: "", to: .metric), "a pinch")
    }

    func testFractionFormatting() {
        XCTAssertEqual(UnitConverter.fractionString(1.5), "1 ½")
        XCTAssertEqual(UnitConverter.fractionString(1.0 / 3), "⅓")
        XCTAssertEqual(UnitConverter.fractionString(2), "2")
        XCTAssertEqual(UnitConverter.fractionString(0.4), "0.4")
    }

    func testPreciseDecimalStrings() {
        XCTAssertEqual(UnitConverter.decimalStringPrecise(1.5), "1.5")
        XCTAssertEqual(UnitConverter.decimalStringPrecise(1.0 / 3), "0.333")
        XCTAssertEqual(UnitConverter.decimalStringPrecise(2), "2")
    }

    /// Android parsed quantities with toDoubleOrNull, so "1/2" scaled to "0".
    func testScalingUnderstandsFractions() {
        let flour = Ingredient(name: "Flour", quantity: "1/2", unit: "cup")
        XCTAssertEqual(ServingsScaler.ingredientText(flour, ratio: 2, system: .imperial), "1 cup")
        XCTAssertEqual(ServingsScaler.ingredientText(flour, ratio: 2, system: .metric), "236.6 ml")
    }

    func testScalingKeepsTextQuantities() {
        let salt = Ingredient(name: "Salt", quantity: "to taste", unit: "")
        XCTAssertEqual(ServingsScaler.ingredientText(salt, ratio: 3, system: .metric), "to taste")
    }

    func testNutritionScaling() {
        XCTAssertEqual(ServingsScaler.nutritionValue("12g", ratio: 2), "24g")
        XCTAssertEqual(ServingsScaler.nutritionValue("250", ratio: 0.5), "125")
        XCTAssertEqual(ServingsScaler.nutritionValue("12.5 g", ratio: 2), "25 g")
        XCTAssertEqual(ServingsScaler.nutritionValue("n/a", ratio: 2), "n/a")
    }
}
