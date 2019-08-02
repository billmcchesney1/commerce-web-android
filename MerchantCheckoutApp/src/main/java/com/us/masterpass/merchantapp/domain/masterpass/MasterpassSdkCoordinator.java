package com.us.masterpass.merchantapp.domain.masterpass;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.mastercard.mp.checkout.AddPaymentMethodRequest;
import com.mastercard.mp.checkout.Amount;
import com.mastercard.mp.checkout.CheckoutResponseConstants;
import com.mastercard.mp.checkout.CryptoOptions;
import com.mastercard.mp.checkout.MasterpassCheckoutCallback;
import com.mastercard.mp.checkout.MasterpassCheckoutRequest;
import com.mastercard.mp.checkout.MasterpassError;
import com.mastercard.mp.checkout.MasterpassInitCallback;
import com.mastercard.mp.checkout.MasterpassMerchant;
import com.mastercard.mp.checkout.MasterpassMerchantConfiguration;
import com.mastercard.mp.checkout.MasterpassPaymentMethod;
import com.mastercard.mp.checkout.NetworkType;
import com.mastercard.mp.checkout.PaymentMethodCallback;
import com.mastercard.mp.checkout.Tokenization;
import com.us.masterpass.merchantapp.BuildConfig;
import com.us.masterpass.merchantapp.R;
import com.us.masterpass.merchantapp.data.device.CartLocalStorage;
import com.us.masterpass.merchantapp.data.device.MerchantPaymentMethod;
import com.us.masterpass.merchantapp.data.device.SettingsSaveConfigurationSdk;
import com.us.masterpass.merchantapp.data.device.SettingsSaveConstants;
import com.us.masterpass.merchantapp.domain.SettingsConstants;
import com.us.masterpass.merchantapp.domain.SettingsListOptions;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Class to handle the SDK calls and handle callbacks and communication between presenters and two
 * specific fragment that need to listen with a callback to receive new data.
 * {@link com.us.masterpass.merchantapp.presentation.fragment.CartFragment}
 * {@link com.us.masterpass.merchantapp.presentation.fragment.SettingsDetailPaymentFragment}
 * <p>
 * Created by Sebastian Farias on 13-10-17.
 */
public class MasterpassSdkCoordinator implements MasterpassCheckoutCallback {

  private static final String TAG = MasterpassSdkCoordinator.class.getSimpleName();
  private static MasterpassSdkCoordinator sMasterpassSdkCoordinator;
  private static Context mContext;
  private static String generatedCartId;
  private static MasterpassUICallback sMasterpassUICallback;
  private static boolean sdkAlreadyInitialized;
  private static final String keyLocalModel = "localCart";
  private HashMap<String, String> environments;

  /**
   * Get instance of class.
   *
   * @return class instance
   */
  public static MasterpassSdkCoordinator getInstance() {
    if (sMasterpassSdkCoordinator == null) {
      synchronized (MasterpassSdkCoordinator.class) {
        sMasterpassSdkCoordinator = new MasterpassSdkCoordinator();
      }
    }
    return sMasterpassSdkCoordinator;
  }

  /**
   * Get express checkout from configuration
   *
   * @return boolean with data from settings
   */
  public static boolean getExpressCheckoutSelected() {
    return SettingsSaveConfigurationSdk.getInstance(mContext)
        .configSwitch(SettingsSaveConstants.SDK_CONFIG_EXPRESS);
  }

  /**
   * Get pairing with checkout from configuration
   *
   * @return boolean with data from settings
   */
  public static boolean getEnableWebCheckoutSelected() {
    return SettingsSaveConfigurationSdk.getInstance(mContext)
        .configSwitch(SettingsSaveConstants.SDK_WEB_CHECKOUT);
  }

  /**
   * Get user id.
   *
   * @return user id if is logged
   */
  public static String getUserId() {
    return SettingsSaveConfigurationSdk.getInstance(mContext).getUserId();
  }

  /**
   * Get pairing id.
   *
   * @return pairing id if exist
   */
  public static String getPairingId() {
    return SettingsSaveConfigurationSdk.getInstance(mContext).getPairingId();
  }

  /**
   * Save pairing id.
   *
   * @param pairingId the pairing id
   */
  public static void savePairingId(String pairingId) {
    SettingsSaveConfigurationSdk.getInstance(mContext).savePairingId(pairingId);
  }

  /**
   * Save pairing id.
   *
   * @param pairingTransactionId the pairing transaction id
   */
  public static void savePairingTransactionId(String pairingTransactionId) {
    SettingsSaveConfigurationSdk.getInstance(mContext)
        .savePairingTransactionId(pairingTransactionId);
  }

  /**
   * Get pairing transaction id.
   *
   * @return pairing id if exist
   */
  public static String getPairingTransactionId() {
    return SettingsSaveConfigurationSdk.getInstance(mContext).getPairingTransactionId();
  }

  /**
   * Get generated cart id.
   *
   * @return generated cart id
   */
  public static String getGeneratedCartId() {
    return generatedCartId;
  }

  /**
   * Option with configuration
   * Initialize the Mastercard SDK, send MasterpassMerchantConfiguration.
   *
   * @param configuration configuration for the SDK {@link MasterpassMerchantConfiguration}
   * @param callback handle initialization {@link MasterpassSdkInterface.GetFromMasterpassSdk}
   */
  public void initializeMasterpassMerchantCallback(MasterpassMerchantConfiguration configuration,
      final MasterpassSdkInterface.GetFromMasterpassSdk callback) {
    MasterpassMerchantConfiguration sConfiguration = configuration;
    try {
      MasterpassMerchant.initialize(sConfiguration, new MasterpassInitCallback() {
        @Override public void onInitSuccess() {
          callback.sdkResponseSuccess();
          Log.d(TAG, "MASTERPASS SDK RUNNING");
        }

        @Override public void onInitError(MasterpassError masterpassError) {
          callback.sdkResponseError("");
          Log.d(TAG, "MASTERPASS SDK ERROR");
        }
      });
    } catch (Exception e) {
      Log.d(TAG, "" +e.getMessage());
    }
  }

  /**
   * Option with context
   * Initialize the Mastercard SDK, send Context
   *
   * @param context Android application context
   * @param callback handle initialization {@link MasterpassSdkInterface.GetFromMasterpassSdk}
   */
  public void initializeMasterpassMerchantCallback(Context context,
      final MasterpassSdkInterface.GetFromMasterpassSdk callback) {

    if (sdkAlreadyInitialized) {
      callback.sdkResponseSuccess();
      return;
    }

    try {
      prepareEnvironments();
      MasterpassMerchant.initialize(getConfigMasterpass(context), new MasterpassInitCallback() {
        @Override public void onInitSuccess() {
          sdkAlreadyInitialized = true;
          callback.sdkResponseSuccess();
          Log.d(TAG, "MASTERPASS SDK RUNNING");
        }

        @Override public void onInitError(MasterpassError masterpassError) {
          callback.sdkResponseError("");
          Log.d(TAG, "MASTERPASS SDK ERROR");
        }
      });
    } catch (Exception e) {
      Log.d(TAG, "" +e.getMessage());
    }
  }

  private void prepareEnvironments() {
    environments = new HashMap<>();
    environments.put("Stage", MasterpassMerchantConfiguration.STAGE);
    environments.put("Stage1", MasterpassMerchantConfiguration.STAGE1);
    environments.put("Stage2", MasterpassMerchantConfiguration.STAGE2);
    environments.put("Stage3", MasterpassMerchantConfiguration.STAGE3);
    environments.put("Itf", MasterpassMerchantConfiguration.ITF);
    environments.put("Int", MasterpassMerchantConfiguration.INT);
    environments.put("Sandbox", MasterpassMerchantConfiguration.SANDBOX);
    environments.put("Prod", MasterpassMerchantConfiguration.PRODUCTION);
  }

  /**
   * Get configuration to initialize SDK, locale comes from saved settings
   *
   * @param context Android application context
   * @return {@link MasterpassMerchantConfiguration}
   */
  private MasterpassMerchantConfiguration getConfigMasterpass(Context context) {
    //TODO check issue with locale SDK Mastercard, to test default flow for web checkout es_CA
    mContext = context;
    String locale = getConfigLocale(mContext);

    return new MasterpassMerchantConfiguration.Builder().setContext(context)
        .setContext(context)
        .setEnvironment(BuildConfig.CHECKOUT_URL)
        .setLocale(
            new Locale(locale.split("_")[0], locale.split("_")[1]))     //SDK Documentation fix
        //.setCheckoutId("1d45705100044e14b52e71730e71cc5a")
        .setCheckoutId(BuildConfig.CHECKOUT_ID)
        .setMerchantName("Merchant Checkout App")
        .setMerchantCountryCode(SettingsListOptions.getCountryCode(context))
        .setExpressCheckoutEnabled(getExpressCheckoutSelected())
        .setAllowedNetworkTypes(getConfigCards())
        .build();
  }

  private boolean getSuppress3DSSelected() {
    return SettingsSaveConfigurationSdk.getInstance(mContext)
            .configSwitch(SettingsSaveConstants.SDK_SUPRESS_3DS);
  }

  private String getMasterpassEnvironment(String enviroment) {
    return environments.get(enviroment);
  }

  /**
   * Get masterpass button on checkout, and normal web flow.
   *
   * @param callback {@link MasterpassSdkInterface.GetFromMasterpassSdkButton}
   */
  public void getMasterpassButton(
      final MasterpassSdkInterface.GetFromMasterpassSdkButton callback) {
    generateCartId();
    try {
      callback.sdkResponseSuccess(MasterpassMerchant.getMasterpassButton(this));
    } catch (Exception e) {
      Log.d(TAG, "MASTERPASS SDK NOT INITIALIZED, WAIT TO SHOW BUTTON");
      callback.sdkResponseError();
    }
  }

  /**
   * Get checkout request on click action
   *
   * @return {@link MasterpassCheckoutRequest}
   */
  @Override public MasterpassCheckoutRequest getCheckoutRequest() {
    double totalPrice = getTotalPrice();

    //DSRP
    ArrayList<String> format = new ArrayList<>();
    CryptoOptions.Mastercard mastercard = new CryptoOptions.Mastercard();
    //CryptoOptions.Visa visa = new CryptoOptions.Visa();
    CryptoOptions cryptoOptions = new CryptoOptions();

    Set<String> selectedDsrp = getSelectedDsrp();
    for (String selected : selectedDsrp) {
      if (!selected.equals("ICC")) {
        format.add(selected);
      }
    }
    mastercard.setFormat(format);
    //visa.setFormat(format);
    cryptoOptions.setMastercard(mastercard);
    //cryptoOptions.setVisa(visa);
    String unpreditableNumber = null;
    try {
      unpreditableNumber =
          Base64.encodeToString(Integer.toString(10000).getBytes("UTF-8"), Base64.NO_WRAP);
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, e.getMessage());
    }
    Tokenization tokenization = new Tokenization(unpreditableNumber, cryptoOptions);

    String totalSalePriceText = String.format("%.2f", totalPrice);
    long doubleToLong = Long.parseLong(totalSalePriceText.replace(".", ""));
    //Amount total = new Amount(doubleToLong, currency);
    //TODO check value when pass decimal data USD$1050, USD$10.50 example for documentation
    Amount total = new Amount(doubleToLong, SettingsListOptions.getCurrenyCode(mContext));
    total.setCurrencyNumber(SettingsListOptions.getCurrenyNumber(mContext));

    //https://sandbox.masterpass.com/routing/v2/mobileapi/web-checkout?
    // checkoutId=6a151ecefd5644dda5529cd9e8ccf1b9&
    // cartId=df88f509-895d-42ec-9635-057990e1665c&amount=25.14&
    // currency=USD&allowedCardTypes=master%2Cvisa&suppressShippingAddress=false&locale=en_US&
    // channel=mobile&masterCryptoFormat=UCAF%2CICC&visaCryptoFormat=TVV

    String userId = getUserId();
    if (userId.length() > 0) {
      return new MasterpassCheckoutRequest.Builder().setMerchantUserId(getUserId())
          .setMerchantUserId(getUserId())
          .setCheckoutId(BuildConfig.CHECKOUT_ID)
          .setCartId(generateCartId())
          .setAmount(total)
          .setMerchantName("MooMerch")
          .setTokenization(tokenization)
          .setAllowedNetworkTypes(getConfigCards())
          //.setSuppress3Ds(getSuppress3DSSelected())
          //.setShippingProfileId("5886541")
          .setCallBackUrl(BuildConfig.CALLBACK_URL)
          .isShippingRequired(getSuppressShipping())
          //.setCvc2Support(false)
          //.setValidityPeriodMinutes(0)
          .build();
    } else {
      return new MasterpassCheckoutRequest.Builder()
          .setCheckoutId(BuildConfig.CHECKOUT_ID)
          .setCartId(generateCartId())
          .setAmount(total)
          .setMerchantName("MooMerch")
          .setTokenization(tokenization)
          //.setSuppress3Ds(getSuppress3DSSelected())
          .setAllowedNetworkTypes(getConfigCards())
          .isShippingRequired(getSuppressShipping())
          //.setShippingProfileId("5886541")
          .setCallBackUrl(BuildConfig.CALLBACK_URL)
          //.setCvc2Support(false)
          //.setValidityPeriodMinutes(0)
          .build();
    }
  }

  /**
   * Callback from SDK, returns a bundle according to the flow selected with expected data.
   * When is called, the data received is sent using {@link MasterpassUICallback} to the fragment
   * {@link com.us.masterpass.merchantapp.presentation.fragment.CartFragment} or
   * {@link com.us.masterpass.merchantapp.presentation.fragment.SettingsDetailPaymentFragment}
   * depending on who has created it
   *
   * @param bundle response Android {@link Bundle}
   */
  @Override public void onCheckoutComplete(final Bundle bundle) {
    Log.d(TAG, "MASTERPASS ONCHECKOUTCOMPLETE BUNDLE ITEM : " + bundle);
    if (bundle != null) {
      HashMap<String, Object> params = new HashMap<>();
      params.put(CheckoutResponseConstants.TRANSACTION_ID,
          bundle.get(CheckoutResponseConstants.TRANSACTION_ID));
      if (bundle.containsKey(CheckoutResponseConstants.PAIRING_TRANSACTION_ID) && getIsLogged()) {
        params.put(CheckoutResponseConstants.PAIRING_TRANSACTION_ID,
            bundle.get(CheckoutResponseConstants.PAIRING_TRANSACTION_ID));
      }
      sMasterpassUICallback.onSDKCheckoutComplete(params);
    }
  }

  @Override public void onCheckoutError(MasterpassError masterpassError) {
    sMasterpassUICallback.onSDKCheckoutError(masterpassError);
  }

  /**
   * Get locale data from personal settings
   *
   * @param context Android application context
   * @return String with locale data
   */
  private String getConfigLocale(Context context) {
    String configLocale = SettingsSaveConfigurationSdk.getInstance(context)
        .configSelectedString(SettingsSaveConstants.SDK_CONFIG_LANG);
    return configLocale;
  }

  /**
   * Get total price of cart
   *
   * @return double with price of cart
   */
  private double getTotalPrice() {
    return CartLocalStorage.getInstance(mContext).getCartTotal(keyLocalModel);
  }

  /**
   * Get DSRP configuration
   *
   * @return String set
   */
  private Set<String> getSelectedDsrp() {
    return SettingsSaveConfigurationSdk.getInstance(mContext)
        .configSelectedStringSet(SettingsSaveConstants.SDK_CONFIG_DSRP);
  }

  /**
   * Get suppress shipping from configuration
   *
   * @return boolean with data from settings
   */
  private boolean getSuppressShipping() {
    return !SettingsSaveConfigurationSdk.getInstance(mContext)
        .configSwitch(SettingsSaveConstants.SDK_CONFIG_SUPRESS);
  }

  /**
   * Get if exist any logged user on the application
   *
   * @return boolean with login status
   */
  private boolean getIsLogged() {
    return SettingsSaveConfigurationSdk.getInstance(mContext).getIsLogged();
  }

  /**
   * Get selected cards from configuration
   *
   * @return ArrayList with cards selected
   */
  private ArrayList<NetworkType> getConfigCards() {
    ArrayList<NetworkType> allowedNetworkTypes = new ArrayList<>();
    Set<String> cards = SettingsSaveConfigurationSdk.getInstance(mContext)
        .configSelectedStringSet(SettingsSaveConstants.SDK_CONFIG_CARDS);
    /*for (String card : cards) {
      if (SettingsConstants.SDK_CARDS.AMERICAN.getConfigToSave().equalsIgnoreCase(card)) {
        allowedNetworkTypes.add(new NetworkType(NetworkType.AMEX));
      } else if (SettingsConstants.SDK_CARDS.DISCOVER.getConfigToSave().equalsIgnoreCase(card)) {
        allowedNetworkTypes.add(new NetworkType(NetworkType.DISCOVER));
      } else if (SettingsConstants.SDK_CARDS.MASTERCARD.getConfigToSave()
          .equalsIgnoreCase(card)) {
        allowedNetworkTypes.add(new NetworkType(NetworkType.MASTER));
      } else if (SettingsConstants.SDK_CARDS.MESTRO.getConfigToSave().equalsIgnoreCase(card)) {
        allowedNetworkTypes.add(new NetworkType(NetworkType.MAESTRO));
      } else if (SettingsConstants.SDK_CARDS.DINERS.getConfigToSave().equalsIgnoreCase(card)) {
        allowedNetworkTypes.add(new NetworkType(NetworkType.DINERS));
      } else if (SettingsConstants.SDK_CARDS.VISA.getConfigToSave().equalsIgnoreCase(card)) {
        allowedNetworkTypes.add(new NetworkType(NetworkType.VISA));
      }
    }*/

    allowedNetworkTypes.add(new NetworkType(NetworkType.MASTER));
    allowedNetworkTypes.add(new NetworkType(NetworkType.VISA));
    allowedNetworkTypes.add(new NetworkType(NetworkType.AMEX));
    return allowedNetworkTypes;
  }

  /**
   * Create random string to set to the Cart Id
   *
   * @return cart id to send to the SDK
   */
  private String generateCartId() {
    SecureRandom rnd = new SecureRandom();
    String generateCartUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String generateCartDigits = "0123456789";
    int generateCartLength = 6;

    String alphanumeric = generateCartUpper + generateCartDigits;
    StringBuilder sb = new StringBuilder(generateCartLength);
    for (int i = 0; i < generateCartLength; i++) {
      sb.append(alphanumeric.charAt(rnd.nextInt(alphanumeric.length())));
    }
    generatedCartId = sb.toString();
    return sb.toString();
  }

  /**
   * Pairing sdk without button.
   *
   * @param callback the callback
   */
  public void pairingSDKWithoutButton(final MasterpassSdkInterface.GetFromMasterpassSdk callback) {
    MasterpassMerchant.pairing(false, this);
  }

  /**
   * Add fragment listener.
   *
   * @param masterpassUICallback the masterpass ui callback
   */
  public void addFragmentListener(MasterpassUICallback masterpassUICallback) {
    synchronized (MasterpassUICallback.class) {
      sMasterpassUICallback = masterpassUICallback;
    }
  }

  /**
   * Remove fragment listener.
   *
   * @param masterpassUICallback the masterpass ui callback
   */
  public void removeFragmentListener(MasterpassUICallback masterpassUICallback) {
    synchronized (MasterpassUICallback.class) {
      sMasterpassUICallback = null;
    }
  }

  /**
   * Retrieve payment methods as json string from shared preference and convert it to payment method
   * list
   *
   * @return List of payment method
   */
  public List<MerchantPaymentMethod> retrievePaymentMethods() {
    return SettingsSaveConfigurationSdk.getInstance(mContext).getPaymentMethod();
  }

  /**
   * AddPayment method call to sdk
   *
   * @param callback onPaymentMethodAdded
   */
  public void addPaymentMethod(final MasterpassSdkInterface.GetMasterpassPaymentMethod callback) {
    MasterpassMerchant.addMasterpassPaymentMethod(new PaymentMethodCallback() {

      @Override public void onPaymentMethodAdded(MasterpassPaymentMethod masterpassPaymentMethod) {
        SettingsSaveConfigurationSdk.getInstance(mContext)
            .savePaymentMethod(masterpassPaymentMethod);
        callback.sdkResponseSuccess(
            SettingsSaveConfigurationSdk.getInstance(mContext).getPaymentMethod());
      }

      @Override public AddPaymentMethodRequest getPaymentMethodRequest() {
        return new AddPaymentMethodRequest(getConfigCards(), BuildConfig.CHECKOUT_ID, getUserId());
      }

      @Override public void onFailure(MasterpassError masterpassError) {
        callback.sdkResponseError(masterpassError);
      }
    });
  }

  /**
   * Deletes payment method from shared preference
   */
  public void deletePaymentMethod(MerchantPaymentMethod paymentMethod,
      final MasterpassSdkInterface.DeleteMasterpassPaymentMethod callback) {
    if (SettingsSaveConfigurationSdk.getInstance(mContext).removePaymentMethod(paymentMethod)) {
      callback.sdkResponseSuccess();
    } else {
      callback.sdkResponseError();
    }
  }

  /**
   * Save selected payment method to shared preference
   */
  public void saveSelectedPaymentMethod(MerchantPaymentMethod paymentMethod,
      final MasterpassSdkInterface.SaveSelectedPaymentMethod callback) {

    if (SettingsSaveConfigurationSdk.getInstance(mContext)
        .saveSelectedPaymentMethod(paymentMethod)) {
      callback.sdkResponseSuccess(
          paymentMethod.getPaymentMethodName() + " added as a Payment method");
    } else {
      callback.sdkResponseError(mContext.getString(R.string.payment_method_error));
    }
  }

  /**
   * get the last selected payment method for checkout
   */
  public MerchantPaymentMethod getSelectedPaymentMethod() {
    return SettingsSaveConfigurationSdk.getInstance(mContext).getSelectedPaymentMethod();
  }

  /**
   * Initiate checkout
   *
   * @param paymentMethodName selected payment method
   * @param callback {@link MasterpassSdkInterface.GetFromMasterpassSdk}
   */
  public void checkout(String paymentMethodName,
      final MasterpassSdkInterface.GetFromMasterpassSdk callback) {
    List<MerchantPaymentMethod> paymentMethodList = retrievePaymentMethods();
    String paymentId = "";
    for (int i = 0; i < paymentMethodList.size(); i++) {
      if (paymentMethodList.get(i).getPaymentMethodName().equals(paymentMethodName)) {
        paymentId = paymentMethodList.get(i).getPaymentMethodId();
        break;
      }
    }
    MasterpassMerchant.paymentMethodCheckout(paymentId, new MasterpassCheckoutCallback() {
      @Override public MasterpassCheckoutRequest getCheckoutRequest() {
        return MasterpassSdkCoordinator.this.getCheckoutRequest();
      }

      @Override public void onCheckoutComplete(Bundle bundle) {
        MasterpassSdkCoordinator.this.onCheckoutComplete(bundle);
      }

      @Override public void onCheckoutError(MasterpassError error) {
        callback.sdkResponseError("error in merchant = " + error.message());
        Log.d(TAG, "error in merchant = " + error.message());
      }

    });
  }

  public static PrivateKey getPublicKey() {
    try {
      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      InputStream keyStoreInputStream =
          mContext.getAssets().open(BuildConfig.MERCHANT_P12_CERTIFICATE);
      keyStore.load(keyStoreInputStream, BuildConfig.PASSWORD.toCharArray());
      return (PrivateKey) keyStore.getKey(BuildConfig.KEY_ALIAS,
          BuildConfig.PASSWORD.toCharArray());
    } catch (Exception e) {
      Log.d("CartFragment", e.toString());
    }
    return null;
  }

  public static boolean isSDKInitialized() {
    return sdkAlreadyInitialized;
  }
}