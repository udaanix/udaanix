/*
 * ============================================================
 * UDAANIX - SHARED AUTHENTICATION HELPERS
 * ============================================================
 *
 * This file contains common authentication-related
 * frontend helpers used across Udaanix authentication pages.
 *
 * IMPORTANT:
 * Student registration API submission is handled inside
 * student-register.html.
 *
 * This file should NOT contain another submit handler for
 * #studentRegisterForm.
 * ============================================================
 */


(function () {

    "use strict";


    /*
     * ========================================================
     * PASSWORD VISIBILITY TOGGLE
     * ========================================================
     *
     * Works with:
     *
     * <button
     *     type="button"
     *     class="password-toggle"
     *     data-target="password">
     *
     * ========================================================
     */

    function initializePasswordToggles() {

        const buttons =
            document.querySelectorAll(".password-toggle");


        buttons.forEach(button => {

            /*
             * Prevent duplicate event listeners
             */
            if (button.dataset.bound === "1") {
                return;
            }


            button.dataset.bound = "1";


            button.addEventListener("click", function () {

                /*
                 * Get target input ID
                 */
                const targetId =
                    button.dataset.target;


                if (!targetId) {
                    return;
                }


                /*
                 * Find password input
                 */
                const input =
                    document.getElementById(targetId);


                if (!input) {
                    return;
                }


                /*
                 * Check current state
                 */
                const isPassword =
                    input.type === "password";


                /*
                 * Change input type
                 */
                input.type =
                    isPassword
                        ? "text"
                        : "password";


                /*
                 * Change icon
                 */
                button.innerHTML =
                    isPassword
                        ? '<i class="fas fa-eye-slash"></i>'
                        : '<i class="fas fa-eye"></i>';


                /*
                 * Accessibility label
                 */
                button.setAttribute(
                    "aria-label",
                    isPassword
                        ? "Hide password"
                        : "Show password"
                );

            });

        });

    }



    /*
     * ========================================================
     * INITIALIZE
     * ========================================================
     */

    if (document.readyState === "loading") {

        document.addEventListener(
            "DOMContentLoaded",
            initializePasswordToggles
        );

    } else {

        initializePasswordToggles();

    }


})();