/* Copyright © 2019 Mastercard. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 =============================================================================*/

package com.mastercard.commerce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Set;

public class CheckoutButtonManager
    implements DownloadCheckoutButton.CheckoutButtonDownloadedListener {
  private static final String TAG  = CheckoutButtonManager.class.getSimpleName();
  private static final String DYNAMIC_BUTTON_IMAGE_URL =
      "https://src.mastercard.com/assets/img/btn/src_chk_btn_376x088px.svg";
  private static volatile CheckoutButtonManager instance;
  private Context context;
  private String checkoutId;
  private Set<CardType> allowedCardTypes;
  private Bitmap checkoutButtonBitmap;
  private CheckoutButton checkoutButton;
  private DataStore dataStore;

  public synchronized static CheckoutButtonManager getInstance() {
    if (instance == null) {
      ConfigurationManager configurationManager = ConfigurationManager.getInstance();
      Context context = configurationManager.getContext();
      String checkoutId = configurationManager.getConfiguration().getCheckoutId();
      Set<CardType> allowedCardTypes =
          configurationManager.getConfiguration().getAllowedCardTypes();
      DataStore dataStore = DataStore.getInstance();

      instance = new CheckoutButtonManager(context, checkoutId, allowedCardTypes, dataStore);
      instance.initialize();
    }

    return instance;
  }

  private void initialize() {
    downloadCheckoutButton();
  }

  public CheckoutButtonManager(Context context, String checkoutId, Set<CardType> allowedCardTypes,
      DataStore dataStore) {
    this.context = context;
    this.checkoutId = checkoutId;
    this.allowedCardTypes = allowedCardTypes;
    this.dataStore = dataStore;
  }

  public CheckoutButton getCheckoutButton(
      CheckoutButton.CheckoutButtonClickListener clickListener) {
    if (checkoutButtonBitmap == null) {
      String checkoutButtonData = readCheckoutButtonFromCache();
      Log.d(TAG,
          "getCheckoutButton , checkoutButtonData = " + checkoutButtonData);
      convertButtonDataToBitmap(checkoutButtonData);
    }
    checkoutButton = new CheckoutButton(context, clickListener, checkoutButtonBitmap);
    return loadDefaultButton(context);
  }

  private void downloadCheckoutButton() {
    String dynamicButtonUrl =
        SrcCheckoutUrlUtil.getDynamicButtonUrl(DYNAMIC_BUTTON_IMAGE_URL, checkoutId,
            allowedCardTypes);
    new DownloadCheckoutButton(dynamicButtonUrl, this).execute();
  }

  private void convertButtonDataToBitmap(String response) {
    Log.d(TAG, "convertButtonDataToBitmap");
    if (response == null || response.isEmpty()) return;
    SVG svg = null;
    try {
      byte[] data = response.getBytes();
      InputStream stream = new ByteArrayInputStream(data);
      svg = SVG.getFromInputStream(stream);
    } catch (SVGParseException e) {
      e.printStackTrace();
    }
    Log.d(TAG, "convertButtonDataToBitmap middle");
    PictureDrawable drawable = new PictureDrawable(svg.renderToPicture());
    checkoutButtonBitmap = pictureDrawableToBitmap(drawable);

    if (checkoutButton == null) {
      Log.d(TAG, "convertButtonDataToBitmap checkoutButton != null");
      //checkoutButton.setBackground(new BitmapDrawable(context.getResources(), checkoutButtonBitmap));
      checkoutButton.setImageBitmap(checkoutButtonBitmap);
    }
  }

  /**
   * This is used to convert the {@link PictureDrawable} to {@link Bitmap}. SVG lib is noticed to
   * have some issue converting to PictureDrawable.
   */
  private Bitmap pictureDrawableToBitmap(PictureDrawable pictureDrawable) {
    Bitmap bmp = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth(),
        pictureDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bmp);
    canvas.drawPicture(pictureDrawable.getPicture());
    return bmp;
  }

  private String getFileName() {
    long hashOfFile = checkoutId.hashCode() + allowedCardTypes.hashCode();
    return (String.valueOf(Math.abs(hashOfFile)) + ".txt");
  }

  private String readCheckoutButtonFromCache() {
    Log.d(TAG, "readCheckoutButtonFromCache");
    File file = new File(context.getCacheDir(), getFileName());
    return dataStore.readFromFileInputStream(file);
  }

  private void writeCheckoutButtonInCache(String checkoutButtonData) {
    File file = new File(context.getCacheDir(), getFileName());
    dataStore.writeDataToFile(file, checkoutButtonData);
  }

  @Override public void checkoutButtonDownloadSuccess(String responseData) {
    writeCheckoutButtonInCache(responseData);
    convertButtonDataToBitmap(responseData);
  }

  @Override public void checkoutButtonDownloadError() {
    //TODO : need to discuss about error scenario
  }

  private CheckoutButton loadDefaultButton(Context context) {
    Log.d(TAG, "loadDefaultButton");
    Bitmap buttonImage = BitmapFactory.decodeResource(context.getResources(), context.getResources()
        .getIdentifier("button_masterpass", "drawable", context.getPackageName()));
    checkoutButton.setImageBitmap(buttonImage);
    return checkoutButton;
  }
}
