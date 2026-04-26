package ai.philterd.policyeditor;

import ai.philterd.phileas.policy.Identifiers;
import ai.philterd.phileas.policy.Policy;
import ai.philterd.phileas.policy.Config;
import ai.philterd.phileas.policy.PostFilters;
import ai.philterd.phileas.policy.config.Pdf;
import ai.philterd.phileas.policy.config.Splitting;
import ai.philterd.phileas.policy.filters.*;
import ai.philterd.phileas.services.strategies.rules.*;
import ai.philterd.phileas.services.strategies.dynamic.*;
import ai.philterd.phileas.services.strategies.ai.*;
import ai.philterd.phileas.services.strategies.custom.CustomDictionaryFilterStrategy;
import ai.philterd.phileas.services.strategies.AbstractFilterStrategy;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class PolicyController {

    private final Gson gson;

    public PolicyController() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapterFactory(new FilterStrategyTypeAdapterFactory())
                .create();
    }

    private static class FilterStrategyTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!AbstractFilterStrategy.class.isAssignableFrom(type.getRawType())) {
                return null;
            }

            final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
            final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    JsonElement tree = delegate.toJsonTree(value);
                    if (tree.isJsonObject()) {
                        JsonObject jsonObject = tree.getAsJsonObject();
                        String strategy = "";
                        if (jsonObject.has("strategy")) {
                            strategy = jsonObject.get("strategy").getAsString();
                        }

                        // Remove anonymizationMethod if not RANDOM_REPLACE
                        if (!"RANDOM_REPLACE".equals(strategy)) {
                            jsonObject.remove("anonymizationMethod");
                        }
                    }
                    elementAdapter.write(out, tree);
                }

                @Override
                public T read(JsonReader in) throws IOException {
                    return delegate.read(in);
                }
            };
        }
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("policyRequest", new PolicyRequest());
        model.addAttribute("piiTypes", Arrays.asList(
            "Age", "Bank Routing Number", "Bitcoin Address", "City", "County", "Credit Card", 
            "Currency", "Date", "Dictionary", "Drivers License", "Email Address", "First Name", "Hospital", 
            "IBAN Code", "IP Address", "Mac Address", "Passport Number", 
            "PhEye", "Phone Number", "Phone Number Extension", "Physician Name", "SSN", 
            "State", "State Abbreviation", "Street Address", "Surname", "Tracking Number", 
            "URL", "VIN", "Zip Code"
        ));
        model.addAttribute("strategies", Arrays.asList("REDACT", "MASK", "RANDOM_REPLACE"));
        return "index";
    }

    @PostMapping("/generate")
    @ResponseBody
    public String generate(@RequestBody PolicyRequest request) throws IOException {
        Policy policy = new Policy();
        
        Identifiers identifiers = new Identifiers();
        List<PhEye> phEyes = new ArrayList<>();
        List<CustomDictionary> customDictionaries = new ArrayList<>();
        
        for (PolicyRequest.FilterSelection selection : request.getFilters()) {
            if ("Age".equalsIgnoreCase(selection.getType())) {
                Age age = new Age();
                List<AgeFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    AgeFilterStrategy strategy = new AgeFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                age.setAgeFilterStrategies(strategies);
                identifiers.setAge(age);
            } else if ("Bank Routing Number".equalsIgnoreCase(selection.getType())) {
                BankRoutingNumber brn = new BankRoutingNumber();
                List<BankRoutingNumberFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    BankRoutingNumberFilterStrategy strategy = new BankRoutingNumberFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                brn.setBankRoutingNumberFilterStrategies(strategies);
                identifiers.setBankRoutingNumber(brn);
            } else if ("Bitcoin Address".equalsIgnoreCase(selection.getType())) {
                BitcoinAddress ba = new BitcoinAddress();
                List<BitcoinAddressFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    BitcoinAddressFilterStrategy strategy = new BitcoinAddressFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                ba.setBitcoinFilterStrategies(strategies);
                identifiers.setBitcoinAddress(ba);
            } else if ("City".equalsIgnoreCase(selection.getType())) {
                City city = new City();
                city.setSensitivity(selection.getSensitivity());
                city.setCapitalized(selection.isCapitalized());
                city.setFuzzy(selection.isFuzzy());
                List<CityFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    CityFilterStrategy strategy = new CityFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                city.setCityFilterStrategies(strategies);
                identifiers.setCity(city);
            } else if ("County".equalsIgnoreCase(selection.getType())) {
                County county = new County();
                county.setSensitivity(selection.getSensitivity());
                county.setCapitalized(selection.isCapitalized());
                county.setFuzzy(selection.isFuzzy());
                List<CountyFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    CountyFilterStrategy strategy = new CountyFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                county.setCountyFilterStrategies(strategies);
                identifiers.setCounty(county);
            } else if ("Credit Card".equalsIgnoreCase(selection.getType())) {
                CreditCard cc = new CreditCard();
                cc.setOnlyValidCreditCardNumbers(selection.isOnlyValidCreditCardNumbers());
                cc.setIgnoreWhenInUnixTimestamp(selection.isIgnoreWhenInUnixTimestamp());
                cc.setOnlyWordBoundaries(selection.isOnlyWordBoundaries());
                List<CreditCardFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    CreditCardFilterStrategy strategy = new CreditCardFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                cc.setCreditCardFilterStrategies(strategies);
                identifiers.setCreditCard(cc);
            } else if ("Currency".equalsIgnoreCase(selection.getType())) {
                Currency currency = new Currency();
                List<CurrencyFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    CurrencyFilterStrategy strategy = new CurrencyFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                currency.setCurrencyFilterStrategies(strategies);
                identifiers.setCurrency(currency);
            } else if ("Date".equalsIgnoreCase(selection.getType())) {
                Date date = new Date();
                date.setOnlyValidDates(selection.isOnlyValidDates());
                List<DateFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    DateFilterStrategy strategy = new DateFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                date.setDateFilterStrategies(strategies);
                identifiers.setDate(date);
            } else if ("Dictionary".equalsIgnoreCase(selection.getType())) {
                CustomDictionary cd = new CustomDictionary();
                cd.setClassification(selection.getClassification());
                cd.setSensitivity(selection.getSensitivity());
                cd.setCapitalized(selection.isCapitalized());
                cd.setFuzzy(selection.isFuzzy());
                cd.setTerms(Arrays.asList("ADD YOUR TERMS HERE"));

                List<CustomDictionaryFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    CustomDictionaryFilterStrategy strategy = new CustomDictionaryFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                cd.setCustomDictionaryFilterStrategies(strategies);
                customDictionaries.add(cd);
            } else if ("Drivers License".equalsIgnoreCase(selection.getType())) {
                DriversLicense dl = new DriversLicense();
                List<DriversLicenseFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    DriversLicenseFilterStrategy strategy = new DriversLicenseFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                dl.setDriversLicenseFilterStrategies(strategies);
                identifiers.setDriversLicense(dl);
            } else if ("Email Address".equalsIgnoreCase(selection.getType())) {
                EmailAddress email = new EmailAddress();
                email.setOnlyStrictMatches(selection.isOnlyStrictMatches());
                email.setOnlyValidTLDs(selection.isOnlyValidTLDs());
                List<EmailAddressFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    EmailAddressFilterStrategy strategy = new EmailAddressFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                email.setEmailAddressFilterStrategies(strategies);
                identifiers.setEmailAddress(email);
            } else if ("First Name".equalsIgnoreCase(selection.getType())) {
                FirstName fn = new FirstName();
                fn.setSensitivity(selection.getSensitivity());
                fn.setCapitalized(selection.isCapitalized());
                fn.setFuzzy(selection.isFuzzy());
                List<FirstNameFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    FirstNameFilterStrategy strategy = new FirstNameFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                fn.setFirstNameFilterStrategies(strategies);
                identifiers.setFirstName(fn);
            } else if ("Hospital".equalsIgnoreCase(selection.getType())) {
                Hospital h = new Hospital();
                h.setSensitivity(selection.getSensitivity());
                h.setCapitalized(selection.isCapitalized());
                h.setFuzzy(selection.isFuzzy());
                List<HospitalFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    HospitalFilterStrategy strategy = new HospitalFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                h.setHospitalFilterStrategies(strategies);
                identifiers.setHospital(h);
            } else if ("IBAN Code".equalsIgnoreCase(selection.getType())) {
                IbanCode iban = new IbanCode();
                iban.setOnlyValidIBANCodes(selection.isOnlyValidIBANCodes());
                iban.setAllowSpaces(selection.isAllowSpaces());
                List<IbanCodeFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    IbanCodeFilterStrategy strategy = new IbanCodeFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                iban.setIbanCodeFilterStrategies(strategies);
                identifiers.setIbanCode(iban);
            } else if ("IP Address".equalsIgnoreCase(selection.getType())) {
                IpAddress ip = new IpAddress();
                List<IpAddressFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    IpAddressFilterStrategy strategy = new IpAddressFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                ip.setIpAddressFilterStrategies(strategies);
                identifiers.setIpAddress(ip);
            } else if ("Mac Address".equalsIgnoreCase(selection.getType())) {
                MacAddress mac = new MacAddress();
                List<MacAddressFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    MacAddressFilterStrategy strategy = new MacAddressFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                mac.setMacAddressFilterStrategies(strategies);
                identifiers.setMacAddress(mac);
            } else if ("Passport Number".equalsIgnoreCase(selection.getType())) {
                PassportNumber pn = new PassportNumber();
                List<PassportNumberFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    PassportNumberFilterStrategy strategy = new PassportNumberFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                pn.setPassportNumberFilterStrategies(strategies);
                identifiers.setPassportNumber(pn);
            } else if ("PhEye".equalsIgnoreCase(selection.getType())) {
                PhEye phEye = new PhEye();
                phEye.setRemovePunctuation(selection.isRemovePunctuation());

                if (selection.getEndpoint() != null) {
                    ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration conf = new ai.philterd.phileas.policy.filters.pheye.PhEyeConfiguration();
                    conf.setEndpoint(selection.getEndpoint());
                    conf.setBearerToken(selection.getBearerToken());
                    if (selection.getTimeout() != null) conf.setTimeout(selection.getTimeout());
                    if (selection.getMaxIdleConnections() != null) conf.setMaxIdleConnections(selection.getMaxIdleConnections());
                    if (selection.getLabels() != null) conf.setLabels(selection.getLabels());
                    phEye.setPhEyeConfiguration(conf);
                }

                List<PhEyeFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    PhEyeFilterStrategy strategy = new PhEyeFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                phEye.setPhEyeFilterStrategies(strategies);
                phEyes.add(phEye);
            } else if ("Phone Number".equalsIgnoreCase(selection.getType())) {
                PhoneNumber pn = new PhoneNumber();
                List<PhoneNumberFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    PhoneNumberFilterStrategy strategy = new PhoneNumberFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                pn.setPhoneNumberFilterStrategies(strategies);
                identifiers.setPhoneNumber(pn);
            } else if ("Phone Number Extension".equalsIgnoreCase(selection.getType())) {
                PhoneNumberExtension pne = new PhoneNumberExtension();
                List<PhoneNumberExtensionFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    PhoneNumberExtensionFilterStrategy strategy = new PhoneNumberExtensionFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                pne.setPhoneNumberExtensionFilterStrategies(strategies);
                identifiers.setPhoneNumberExtension(pne);
            } else if ("Physician Name".equalsIgnoreCase(selection.getType())) {
                PhysicianName pn = new PhysicianName();
                List<PhysicianNameFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    PhysicianNameFilterStrategy strategy = new PhysicianNameFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                pn.setPhysicianNameFilterStrategies(strategies);
                identifiers.setPhysicianName(pn);
            } else if ("SSN".equalsIgnoreCase(selection.getType())) {
                Ssn ssn = new Ssn();
                List<SsnFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    SsnFilterStrategy strategy = new SsnFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                ssn.setSsnFilterStrategies(strategies);
                identifiers.setSsn(ssn);
            } else if ("State".equalsIgnoreCase(selection.getType())) {
                State state = new State();
                state.setSensitivity(selection.getSensitivity());
                state.setCapitalized(selection.isCapitalized());
                state.setFuzzy(selection.isFuzzy());
                List<StateFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    StateFilterStrategy strategy = new StateFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                state.setStateFilterStrategies(strategies);
                identifiers.setState(state);
            } else if ("State Abbreviation".equalsIgnoreCase(selection.getType())) {
                StateAbbreviation sa = new StateAbbreviation();
                List<StateAbbreviationFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    StateAbbreviationFilterStrategy strategy = new StateAbbreviationFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                sa.setStateAbbreviationsFilterStrategies(strategies);
                identifiers.setStateAbbreviation(sa);
            } else if ("Street Address".equalsIgnoreCase(selection.getType())) {
                StreetAddress sa = new StreetAddress();
                List<StreetAddressFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    StreetAddressFilterStrategy strategy = new StreetAddressFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                sa.setStreetAddressFilterStrategies(strategies);
                identifiers.setStreetAddress(sa);
            } else if ("Surname".equalsIgnoreCase(selection.getType())) {
                Surname surname = new Surname();
                surname.setSensitivity(selection.getSensitivity());
                surname.setCapitalized(selection.isCapitalized());
                surname.setFuzzy(selection.isFuzzy());
                List<SurnameFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    SurnameFilterStrategy strategy = new SurnameFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                surname.setSurnameFilterStrategies(strategies);
                identifiers.setSurname(surname);
            } else if ("Tracking Number".equalsIgnoreCase(selection.getType())) {
                TrackingNumber tn = new TrackingNumber();
                tn.setUps(selection.isUps());
                tn.setFedex(selection.isFedex());
                tn.setUsps(selection.isUsps());
                tn.setAllowSpaces(selection.isAllowSpaces());
                List<TrackingNumberFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    TrackingNumberFilterStrategy strategy = new TrackingNumberFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                tn.setTrackingNumberFilterStrategies(strategies);
                identifiers.setTrackingNumber(tn);
            } else if ("URL".equalsIgnoreCase(selection.getType())) {
                Url url = new Url();
                List<UrlFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    UrlFilterStrategy strategy = new UrlFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                url.setUrlFilterStrategies(strategies);
                identifiers.setUrl(url);
            } else if ("VIN".equalsIgnoreCase(selection.getType())) {
                Vin vin = new Vin();
                List<VinFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    VinFilterStrategy strategy = new VinFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    strategies.add(strategy);
                }
                vin.setVinFilterStrategies(strategies);
                identifiers.setVin(vin);
            } else if ("Zip Code".equalsIgnoreCase(selection.getType())) {
                ZipCode zc = new ZipCode();
                zc.setRequireDelimiter(selection.isRequireDelimiter());
                zc.setValidate(selection.isValidate());

                List<ZipCodeFilterStrategy> strategies = new ArrayList<>();
                for (PolicyRequest.StrategySelection s : selection.getStrategies()) {
                    ZipCodeFilterStrategy strategy = new ZipCodeFilterStrategy();
                    strategy.setStrategy(s.getStrategy());
                    strategy.setConditions(s.getCondition());
                    if (s.getTruncateLeaveCharacters() != null) {
                        strategy.setTruncateLeaveCharacters(s.getTruncateLeaveCharacters());
                    }
                    if (s.getTruncateDigits() != null) {
                        strategy.setTruncateLeaveCharacters(s.getTruncateDigits());
                    }
                    strategies.add(strategy);
                }

                zc.setZipCodeFilterStrategies(strategies);
                identifiers.setZipCode(zc);
            }
        }
        
        policy.setIdentifiers(identifiers);
        identifiers.setPhEyes(phEyes);
        identifiers.setCustomDictionaries(customDictionaries);

        Config config = new Config();
        PostFilters postFilters = new PostFilters();
        postFilters.setRemoveTrailingPeriods(request.getPostFilters().isRemoveTrailingPeriods());
        postFilters.setRemoveTrailingSpaces(request.getPostFilters().isRemoveTrailingSpaces());
        postFilters.setRemoveTrailingNewLines(request.getPostFilters().isRemoveTrailingNewLines());
        config.setPostFilters(postFilters);

        Pdf pdf = new Pdf();
        pdf.setRedactionColor(request.getPdf().getRedactionColor());
        pdf.setReplacementFont(request.getPdf().getReplacementFont());
        pdf.setShowReplacement(request.getPdf().isShowReplacement());
        pdf.setScale(request.getPdf().getScale());
        pdf.setDpi(request.getPdf().getDpi());
        pdf.setCompressionQuality(request.getPdf().getCompressionQuality());
        pdf.setPreserveUnredactedPages(request.getPdf().isPreserveUnredactedPages());
        config.setPdf(pdf);

        Splitting splitting = new Splitting();
        splitting.setEnabled(request.getSplitting().isEnabled());
        splitting.setMethod(request.getSplitting().getMethod());
        splitting.setThreshold(request.getSplitting().getThreshold());
        config.setSplitting(splitting);

        policy.setConfig(config);
        
        return gson.toJson(policy);
    }
}
