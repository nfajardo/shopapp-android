package com.shopify.ui.checkout.contract

import com.domain.entity.*
import com.domain.interactor.account.GetCustomerUseCase
import com.domain.interactor.cart.CartItemsUseCase
import com.domain.interactor.cart.CartRemoveAllUseCase
import com.shopify.constant.CARD_PAYMENT
import com.shopify.entity.Checkout
import com.shopify.entity.ShippingRate
import com.shopify.interactor.checkout.*
import com.ui.base.contract.BaseLcePresenter
import com.ui.base.contract.BaseLceView
import javax.inject.Inject

interface CheckoutView : BaseLceView<Checkout> {

    fun customerReceived(customer: Customer?)

    fun cartProductListReceived(cartProductList: List<CartProduct>)

    fun shippingRatesReceived(shippingRates: List<ShippingRate>)

    fun checkoutValidationPassed(isValid: Boolean)

    fun checkoutInProcess()

    fun checkoutCompleted(order: Order)

    fun checkoutError()
}

class CheckoutPresenter @Inject constructor(
    private val cartItemsUseCase: CartItemsUseCase,
    private val cartRemoveAllUseCase: CartRemoveAllUseCase,
    private val createCheckoutUseCase: CreateCheckoutUseCase,
    private val getCheckoutUseCase: GetCheckoutUseCase,
    private val getCustomerUseCase: GetCustomerUseCase,
    private val setShippingAddressUseCase: SetShippingAddressUseCase,
    private val getShippingRatesUseCase: GetShippingRatesUseCase,
    private val setShippingRateUseCase: SetShippingRateUseCase,
    private val completeCheckoutByCardUseCase: CompleteCheckoutByCardUseCase
) :
    BaseLcePresenter<Checkout, CheckoutView>(
        cartItemsUseCase,
        cartRemoveAllUseCase,
        createCheckoutUseCase,
        getCheckoutUseCase,
        getCustomerUseCase,
        setShippingAddressUseCase,
        getShippingRatesUseCase,
        setShippingRateUseCase,
        completeCheckoutByCardUseCase
    ) {

    fun getCartProductList() {
        cartItemsUseCase.execute(
            {
                view?.cartProductListReceived(it)
                createCheckout(it)
            },
            { resolveError(it) },
            {},
            Unit
        )
    }

    private fun createCheckout(cartProductList: List<CartProduct>) {
        createCheckoutUseCase.execute(
            { getCustomer(it) },
            { resolveError(it) },
            cartProductList
        )
    }

    private fun getCustomer(checkout: Checkout) {
        getCustomerUseCase.execute(
            {
                view?.customerReceived(it)
                val defaultAddress = it.defaultAddress
                if (defaultAddress != null) {
                    setShippingAddress(checkout, defaultAddress)
                } else {
                    view?.showContent(checkout)
                }
            },
            {
                it.printStackTrace()
                view?.showContent(checkout)
                view?.customerReceived(null)
            },
            Unit
        )
    }

    private fun setShippingAddress(checkout: Checkout, address: Address) {
        setShippingAddressUseCase.execute(
            {
                view?.showContent(it)
            },
            {
                it.printStackTrace()
                view?.showContent(checkout)
            },
            SetShippingAddressUseCase.Params(checkout.checkoutId, address)
        )
    }

    fun getCheckout(checkoutId: String) {
        getCheckoutUseCase.execute(
            { view?.showContent(it) },
            { resolveError(it) },
            checkoutId
        )
    }

    fun getShippingRates(checkoutId: String) {
        getShippingRatesUseCase.execute(
            { view?.shippingRatesReceived(it) },
            { it.printStackTrace() },
            checkoutId
        )
    }

    fun setShippingRates(checkout: Checkout, shippingRate: ShippingRate) {
        setShippingRateUseCase.execute(
            { view?.showContent(it) },
            { view?.showContent(checkout) },
            SetShippingRateUseCase.Params(checkout.checkoutId, shippingRate)
        )
    }

    fun verifyCheckoutData(
        checkout: Checkout?,
        email: String?,
        paymentType: String?,
        card: Card?,
        cardToken: String?,
        billingAddress: Address?
    ) {
        if (checkout != null) {
            if (checkout.address == null) {
                view?.checkoutValidationPassed(false)
                return
            }
            if (email == null) {
                view?.checkoutValidationPassed(false)
                return
            }
            if (checkout.shippingRate == null) {
                view?.checkoutValidationPassed(false)
                return
            }
            if (paymentType == null) {
                view?.checkoutValidationPassed(false)
                return
            } else if (paymentType == CARD_PAYMENT
                && (card == null || cardToken == null || billingAddress == null)) {
                view?.checkoutValidationPassed(false)
                return
            }
            view?.checkoutValidationPassed(true)
        } else {
            view?.checkoutValidationPassed(false)
        }
    }

    fun completeCheckoutByCard(
        checkout: Checkout?,
        email: String?,
        billingAddress: Address?,
        cardToken: String?
    ) {
        if (checkout != null && email != null && billingAddress != null && cardToken != null) {
            view?.checkoutInProcess()
            completeCheckoutByCardUseCase.execute(
                {
                    cartRemoveAllUseCase.execute({}, {}, Unit)
                    view?.checkoutCompleted(it)
                },
                {
                    view?.checkoutError()
                },
                CompleteCheckoutByCardUseCase.Params(checkout, email, billingAddress, cardToken)
            )
        }
    }
}