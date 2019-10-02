package com.mastercard.testapp.domain.usecase.base;

/**
 * Use cases are the entry points to the domain layer, all use case must need extends from this.
 *
 * @param <Q> the request type
 * @param <P> the response type
 */
public abstract class UseCase<Q extends UseCase.RequestValues, P extends UseCase.ResponseValue> {

  private Q mRequestValues;
  private UseCaseCallback<P> mUseCaseCallback;

  /**
   * Gets request values.
   *
   * @return the request values
   */
  public Q getRequestValues() {
    return mRequestValues;
  }

  /**
   * Sets request values.
   *
   * @param requestValues the request values
   */
  public void setRequestValues(Q requestValues) {
    mRequestValues = requestValues;
  }

  /**
   * Gets use case callback.
   *
   * @return the use case callback
   */
  public UseCaseCallback<P> getUseCaseCallback() {
    return mUseCaseCallback;
  }

  /**
   * Sets use case callback.
   *
   * @param useCaseCallback the use case callback
   */
  public void setUseCaseCallback(UseCaseCallback<P> useCaseCallback) {
    mUseCaseCallback = useCaseCallback;
  }

  /**
   * Run.
   */
  void run() {
    executeUseCase(mRequestValues);
  }

  /**
   * Execute use case.
   *
   * @param requestValues the request values
   */
  protected abstract void executeUseCase(Q requestValues);

  /**
   * The interface Request values.
   */
  public interface RequestValues {
  }

  /**
   * The interface Response value.
   */
  public interface ResponseValue {
  }

  /**
   * The interface Use case callback.
   *
   * @param <R> the type parameter
   */
  public interface UseCaseCallback<R> {
    /**
     * On success.
     *
     * @param response the response
     */
    void onSuccess(R response);

    /**
     * On error.
     */
    void onError();
  }
}