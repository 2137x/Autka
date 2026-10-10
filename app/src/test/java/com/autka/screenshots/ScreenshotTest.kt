package com.autka.screenshots

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.autka.core.model.CarOffer
import com.autka.core.model.Currency
import com.autka.core.model.ExchangeRates
import com.autka.core.model.FuelType
import com.autka.core.model.ImportCostCalculator
import com.autka.core.model.Money
import com.autka.core.model.Region
import com.autka.core.model.SourceHealth
import com.autka.core.model.Transmission
import com.autka.core.model.VinDecodeResult
import com.autka.data.repository.SourceInfo
import com.autka.feature.detail.OfferDetailScreen
import com.autka.feature.detail.OfferDetailUiState
import com.autka.feature.importcalc.ImportCalculatorScreen
import com.autka.feature.listings.ListingsScreen
import com.autka.feature.listings.ListingsUiState
import com.autka.feature.sourcehealth.SourceHealthScreen
import com.autka.feature.sourcehealth.SourceHealthUiState
import com.autka.feature.vin.VinDecoderScreen
import com.autka.feature.vin.VinDecoderUiState
import com.autka.ui.theme.AutkaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM screenshots of the stateless screens with fixture state, for reviewing UI changes without
 * an emulator. Record: `./gradlew :app:recordRoborazziDebug --tests '*ScreenshotTest*'`; PNGs
 * land in app/build/outputs/roborazzi. Fixtures carry no image URLs, so output is deterministic.
 * MapScreen is left out: MapLibre needs its native renderer.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application: the real one is Hilt-generated and boots MapLibre's native library.
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi", application = Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun capture(
        name: String,
        dark: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        compose.setContent { AutkaTheme(darkTheme = dark) { content() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$OUT/$name.png")
    }

    @Composable
    private fun Listings(state: ListingsUiState) =
        ListingsScreen(
            uiState = state,
            onQueryChange = {},
            onSearch = {},
            onApplyFilter = {},
            onApplySavedSearch = {},
            onResetFilter = {},
            onSaveSearch = { _, _, _ -> },
            onDeleteSavedSearch = {},
            onDisplayCurrencyChange = {},
            onOfferClick = {},
            onMapClick = {},
            onImportCalculatorClick = {},
            onVinDecoderClick = {},
            onSourceHealthClick = {},
        )

    private val listings =
        ListingsUiState(
            offers = OFFERS,
            availableMakes = OFFERS.map { it.make }.distinct(),
            availableSources = listOf(SourceInfo("mock", "Sample data", enabled = true)),
            exchangeRates = RATES,
        )

    @Test fun listingsLight() = capture("listings_light") { Listings(listings) }

    @Test fun listingsDark() = capture("listings_dark", dark = true) { Listings(listings) }

    @Test
    fun listingsEmptyWithFailedSource() =
        capture("listings_empty") {
            Listings(ListingsUiState(failedSources = listOf("Otomoto"), errorMessage = "Network error"))
        }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-xhdpi")
    fun listingsTablet() = capture("listings_tablet") { Listings(listings) }

    @Test
    fun detailPoland() =
        capture("detail_poland") {
            OfferDetailScreen(
                uiState = OfferDetailUiState.Success(OFFERS[0], null, Currency.PLN, RATES),
                onBack = {},
            )
        }

    @Test
    fun detailUsaImport() =
        capture("detail_usa_import") {
            val usa = OFFERS[2]
            OfferDetailScreen(
                uiState =
                    OfferDetailUiState.Success(
                        offer = usa,
                        importEstimate = usa.importEstimate,
                        displayCurrency = Currency.PLN,
                        exchangeRates = RATES,
                        shippingUsd = 2_400.0,
                        engineCapacityCc = 5000,
                    ),
                onBack = {},
            )
        }

    @Test
    fun sourceHealth() =
        capture("source_health") {
            SourceHealthScreen(
                uiState =
                    SourceHealthUiState(
                        isLoading = false,
                        sources =
                            listOf(
                                SourceHealth("otomoto", "Otomoto", true, 1240, NOW - 3_600_000L, true, 37),
                                SourceHealth("autoscout", "AutoScout24", true, 0, NOW - 86_400_000L, false, 0),
                                SourceHealth("copart", "Copart", false, null, null, null, null),
                            ),
                    ),
                onBack = {},
                onRefresh = {},
            )
        }

    @Test
    fun vinDecoded() =
        capture("vin_decoded") {
            VinDecoderScreen(
                uiState =
                    VinDecoderUiState(
                        vin = "WBA8E1C51JA123456",
                        result =
                            VinDecodeResult(
                                vin = "WBA8E1C51JA123456",
                                make = "BMW",
                                model = "3 Series",
                                modelYear = "2018",
                            ),
                    ),
                onVinChange = {},
                onDecode = {},
                onBack = {},
            )
        }

    @Test
    fun importCalculator() =
        capture("import_calculator") {
            ImportCalculatorScreen(
                onBack = {},
                displayCurrency = Currency.PLN,
                exchangeRates = RATES,
                presets = emptyList(),
                onSavePreset = { _, _, _, _ -> },
                onDeletePreset = {},
            )
        }

    private companion object {
        const val OUT = "build/outputs/roborazzi"
        const val NOW = 1_760_000_000_000L
        val RATES =
            ExchangeRates(
                base = Currency.PLN,
                perUnit = mapOf(Currency.PLN to 1.0, Currency.EUR to 4.25, Currency.USD to 3.65),
                asOfEpochMs = NOW,
                isStale = false,
            )

        private fun offer(
            n: Int,
            title: String,
            make: String,
            model: String,
            price: Money,
            region: Region,
            location: String,
            fuel: FuelType = FuelType.PETROL,
        ) = CarOffer(
            id = "mock:$n",
            sourceId = "mock",
            title = title,
            make = make,
            model = model,
            year = 2018 + n,
            mileageKm = 150_000 - n * 40_000,
            price = price,
            fuelType = fuel,
            transmission = Transmission.AUTOMATIC,
            powerHp = 190,
            location = location,
            region = region,
            thumbnailUrl = null,
            imageUrls = emptyList(),
            listingUrl = "https://example.com/listing/$n",
            postedAtEpochMs = NOW - n * 3_600_000L,
        )

        val OFFERS =
            listOf(
                offer(0, "BMW 320d 2018 Touring", "BMW", "320d", Money(78_900.0, Currency.PLN), Region.POLAND, "Kraków, PL", FuelType.DIESEL),
                offer(1, "Audi A4 2.0 TFSI 2019", "Audi", "A4", Money(19_500.0, Currency.EUR), Region.EUROPE, "Berlin, DE"),
                offer(2, "Ford Mustang GT 5.0 2020 (salvage)", "Ford", "Mustang", Money(18_000.0, Currency.USD), Region.USA, "Newark, NJ, USA")
                    .copy(
                        importEstimate =
                            ImportCostCalculator.estimate(
                                vehiclePriceUsd = 18_000.0,
                                shippingUsd = 2_400.0,
                                engineCapacityCc = 5000,
                            ),
                    ),
            )
    }
}
