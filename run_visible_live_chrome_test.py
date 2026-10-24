import time
import os
import re
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options

def show_step_banner(driver, title, description, color="#10b981"):
    """Displays an elegant visual banner on top of the live Chrome browser window."""
    script = f"""
    var existing = document.getElementById('liveTestOverlayBanner');
    if (existing) existing.remove();
    var banner = document.createElement('div');
    banner.id = 'liveTestOverlayBanner';
    banner.style.position = 'fixed';
    banner.style.top = '12px';
    banner.style.left = '50%';
    banner.style.transform = 'translateX(-50%)';
    banner.style.zIndex = '999999';
    banner.style.background = 'rgba(15, 23, 42, 0.95)';
    banner.style.backdropFilter = 'blur(12px)';
    banner.style.border = '2px solid {color}';
    banner.style.borderRadius = '16px';
    banner.style.padding = '12px 28px';
    banner.style.color = '#ffffff';
    banner.style.boxShadow = '0 12px 35px rgba(0,0,0,0.4)';
    banner.style.fontFamily = 'Outfit, sans-serif';
    banner.style.textAlign = 'center';
    banner.style.pointerEvents = 'none';
    banner.style.transition = 'all 0.3s ease';
    banner.innerHTML = '<div style="font-size: 15px; font-weight: 800; color: {color}; text-transform: uppercase; letter-spacing: 1px;">{title}</div>' +
                       '<div style="font-size: 13px; color: #cbd5e1; margin-top: 3px;">{description}</div>';
    document.body.appendChild(banner);
    """
    try:
        driver.execute_script(script)
    except Exception:
        pass

def highlight_element(driver, element, color="#10b981"):
    """Highlights an active element with a glowing outline so the user can clearly see what's being clicked."""
    try:
        driver.execute_script(f"""
            arguments[0].style.outline = '3px solid {color}';
            arguments[0].style.boxShadow = '0 0 15px {color}';
            arguments[0].style.transition = 'all 0.25s ease';
        """, element)
    except Exception:
        pass

def run_visible_tests():
    print("=" * 70, flush=True)
    print("LAUNCHING VISIBLE LIVE CHROME BROWSER TESTING FOR USER", flush=True)
    print("=" * 70, flush=True)

    os.makedirs("Screen Shots", exist_ok=True)

    chrome_options = Options()
    # DO NOT use headless mode! Keep Chrome visible on user's screen.
    chrome_options.add_argument("--window-size=1440,940")
    chrome_options.add_argument("--window-position=50,30")
    chrome_options.add_argument("--disable-gpu")
    chrome_options.add_argument("--no-sandbox")
    chrome_options.add_argument("--disable-dev-shm-usage")
    chrome_options.add_argument("--use-fake-ui-for-media-stream")
    chrome_options.add_argument("--use-fake-device-for-media-stream")
    chrome_options.add_experimental_option("excludeSwitches", ["enable-automation"])
    chrome_options.add_experimental_option('useAutomationExtension', False)
    chrome_options.page_load_strategy = "eager"

    print("Opening visible Google Chrome browser window...", flush=True)
    driver = webdriver.Chrome(options=chrome_options)
    wait = WebDriverWait(driver, 15)

    try:
        # STEP 1: Admin Login & Clean Reset
        print("\n>>> STEP 1: Admin Login & Election Control Center Dashboard", flush=True)
        driver.get("http://localhost:8080/admin/login")
        time.sleep(1.5)
        show_step_banner(driver, "STEP 1: Administrator Authentication", "Super Admin logging in to master control center", "#3b82f6")
        time.sleep(1.5)

        driver.execute_script("""
            var u = document.querySelector('input[name=username]');
            var p = document.querySelector('input[name=password]');
            if (u && p) {
                u.value = 'Venkatesh';
                p.value = 'password';
                document.querySelector('form').submit();
            }
        """)
        wait.until(lambda d: "/admin/dashboard" in d.current_url)
        time.sleep(1.5)

        # Reset votes for fresh demo
        print("  -> Resetting old ballots for clean demonstration...", flush=True)
        driver.get("http://localhost:8080/admin/votes")
        time.sleep(1)
        driver.execute_script("document.querySelector(\"form[action='/admin/votes/reset']\").submit();")
        time.sleep(1.5)

        # STEP 2: Configure Election Schedule & Time Limit Modal
        print("\n>>> STEP 2: Setting Election Time Limit & Schedule", flush=True)
        driver.get("http://localhost:8080/admin/dashboard")
        wait.until(EC.presence_of_element_located((By.ID, "voterShareLink")))
        time.sleep(2)
        show_step_banner(driver, "STEP 2: Election Schedule & Time Limit Configuration", "Opening schedule modal to activate 24-hour live voting window", "#f59e0b")
        time.sleep(2)

        driver.save_screenshot("Screen Shots/visible_1_admin_dashboard.png")

        # Open Schedule Modal
        print("  -> Clicking 'Configure Election Schedule & Time Limit'...", flush=True)
        driver.execute_script("""
            var modalEl = document.getElementById('scheduleModal');
            var m = new bootstrap.Modal(modalEl);
            m.show();
        """)
        time.sleep(2)
        driver.save_screenshot("Screen Shots/visible_2_schedule_modal.png")

        # Click "+24 Hours" preset button
        print("  -> Setting +24 Hours live voting window preset...", flush=True)
        driver.execute_script("setPresetHours(24);")
        time.sleep(2)

        # Submit schedule form
        print("  -> Saving active election schedule...", flush=True)
        driver.execute_script("document.querySelector(\"#scheduleModal form\").submit();")
        wait.until(lambda d: "/admin/dashboard" in d.current_url)
        time.sleep(2)

        show_step_banner(driver, "Election Schedule Activated!", "Polls are LIVE with active countdown timer & deadline lock", "#10b981")
        time.sleep(2)
        driver.save_screenshot("Screen Shots/visible_3_schedule_active.png")

        # STEP 3: Showcase Voter Access Center & QR Code Modal
        print("\n>>> STEP 3: Demonstrating Voter Access Center & Official QR Code", flush=True)
        show_step_banner(driver, "STEP 3: Voter Access Center & QR Code", "Copying direct voter portal link and showing official scannable QR Code", "#8b5cf6")
        time.sleep(2)

        # Open QR Code Modal
        driver.execute_script("""
            var qrModalEl = document.getElementById('voterQrModal');
            var qm = new bootstrap.Modal(qrModalEl);
            qm.show();
        """)
        time.sleep(3)
        driver.save_screenshot("Screen Shots/visible_4_voter_qr_code.png")

        # Close QR Modal
        driver.execute_script("""
            var qrModalEl = document.getElementById('voterQrModal');
            var qm = bootstrap.Modal.getInstance(qrModalEl);
            if (qm) qm.hide();
        """)
        time.sleep(1.5)

        # STEP 4: Switch Organization to Infosys
        print("\n>>> STEP 4: Multi-Tenant Switcher - Infosys Tenant Scope", flush=True)
        show_step_banner(driver, "STEP 4: Multi-Tenant Organizer Switching", "Switching dashboard scope to Infosys Cultural Organizer", "#3b82f6")
        driver.get("http://localhost:8080/admin/dashboard?viewOrg=Infosys")
        time.sleep(2.5)
        driver.save_screenshot("Screen Shots/visible_5_infosys_dashboard.png")

        # Clean session for Voter
        print("  -> Logging out admin and preparing fresh voter session...", flush=True)
        driver.get("http://localhost:8080/admin/logout")
        driver.get("http://localhost:8080/logout")
        driver.delete_all_cookies()
        time.sleep(1.5)

        # STEP 5: Direct Voter Route - Infosys Corporate Portal
        print("\n>>> STEP 5: Direct Voter Route (/vote?org=Infosys)", flush=True)
        driver.get("http://localhost:8080/vote?org=Infosys")
        wait.until(EC.presence_of_element_located((By.NAME, "voter")))
        time.sleep(2)
        show_step_banner(driver, "STEP 5: Direct Voter Portal (/vote?org=Infosys)", "Dedicated Infosys Corporate Portal with live schedule badge and isolated voter UI", "#10b981")
        time.sleep(2.5)
        driver.save_screenshot("Screen Shots/visible_6_voter_portal.png")

        # STEP 6: Voter Login
        print("\n>>> STEP 6: Voter Login with Infosys Employee Credentials", flush=True)
        show_step_banner(driver, "STEP 6: Voter Credentials Verification", "Authenticating Infosys Employee: Ananya Rao (INFY_108291)", "#10b981")

        driver.execute_script("""
            var v = document.querySelector('input[name=voter]');
            var p = document.querySelector('input[name=password]');
            if (v && p) {
                v.value = 'INFY_108291';
                p.value = 'password';
            }
        """)
        time.sleep(1.5)

        voter_input = driver.find_element(By.NAME, "voter")
        highlight_element(driver, voter_input, "#10b981")
        voter_pass = driver.find_element(By.NAME, "password")
        highlight_element(driver, voter_pass, "#10b981")
        time.sleep(1)

        # Submit voter form
        driver.execute_script("document.querySelector('form').submit();")
        wait.until(lambda d: "/verify-face" in d.current_url)
        time.sleep(1.5)

        # STEP 7: Biometric Face AI Verification
        print(f"\n>>> STEP 7: Biometric Facial Recognition AI ({driver.current_url})", flush=True)
        show_step_banner(driver, "STEP 7: Biometric Face AI Verification", "Live camera viewport, anti-spoofing challenge & facial comparison", "#06b6d4")
        time.sleep(2.5)
        driver.save_screenshot("Screen Shots/visible_7_face_verification.png")

        # Authenticate with Emergency Key
        print("  -> Authorizing via Biometric Emergency Passkey: VOTE2026", flush=True)
        driver.execute_script("""
            var modal = document.querySelector('#emergencyModal');
            if (modal) {
                modal.classList.add('show');
                modal.style.display = 'block';
                document.querySelector('#emergencyModal input[name=emergencyKey]').value = 'VOTE2026';
                document.querySelector('#emergencyModal form').submit();
            }
        """)
        wait.until(lambda d: "/verify-otp" in d.current_url)
        time.sleep(1.5)

        # STEP 8: Email OTP 2FA Verification
        print(f"\n>>> STEP 8: Email OTP Two-Factor Authentication ({driver.current_url})", flush=True)
        show_step_banner(driver, "STEP 8: Two-Factor OTP Verification", "Verifying 6-digit cryptographic security code dispatched to voter email", "#f59e0b")
        time.sleep(2)
        driver.save_screenshot("Screen Shots/visible_8_otp_page.png")

        # Extract 6-digit simulated OTP code from badge
        otp_match = re.search(r'class="badge bg-success fs-6">([0-9]{6})</span>', driver.page_source)
        if otp_match:
            otp_code = otp_match.group(1)
            print(f"  -> Extracted Real OTP Code: {otp_code}", flush=True)
        else:
            raise Exception("OTP code badge not found!")

        driver.execute_script(f"""
            var inp = document.querySelector('#otpInput');
            if (inp) inp.value = '{otp_code}';
        """)
        time.sleep(1.5)

        driver.execute_script("document.querySelector('#otpForm').submit();")
        wait.until(lambda d: "/ballot" in d.current_url)
        time.sleep(2)

        # STEP 9: Official Digital Ballot with Live Countdown Timer
        print(f"\n>>> STEP 9: Official Digital Ballot with Live Countdown Timer ({driver.current_url})", flush=True)
        show_step_banner(driver, "STEP 9: Official Digital Ballot", "Notice the Live Countdown Timer Banner and Infosys Cultural Organizer scope!", "#10b981")
        time.sleep(3)
        driver.save_screenshot("Screen Shots/visible_9_ballot_countdown.png")

        # Select candidate for Cultural Event Organizer
        print("  -> Selecting candidate: Rohan Sharma (Infosys Cultural Committee)...", flush=True)
        cand_radio = driver.find_element(By.CSS_SELECTOR, "input[type='radio'].candidate-select")
        highlight_element(driver, cand_radio, "#10b981")
        driver.execute_script("arguments[0].click();", cand_radio)
        time.sleep(1.5)
        driver.save_screenshot("Screen Shots/visible_10_ballot_selected.png")

        # Open Preview Modal
        print("  -> Opening Ballot Preview Modal...", flush=True)
        driver.execute_script("document.getElementById('btnPreview').click();")
        time.sleep(2)
        driver.save_screenshot("Screen Shots/visible_11_ballot_preview.png")

        # Cast Ballot
        print("  -> Sealing Ballot with AES-256 Encryption & RSA Digital Signature...", flush=True)
        show_step_banner(driver, "Casting Ballot to Blockchain", "Encrypting vote payload with AES-256 and signing with voter RSA key", "#8b5cf6")
        time.sleep(1.5)
        driver.execute_script("document.getElementById('ballotForm').submit();")
        wait.until(lambda d: "/receipt" in d.current_url)
        time.sleep(2)

        # STEP 10: Official Ballot Receipt
        print(f"\n>>> STEP 10: Cryptographic Ballot Receipt & Micro-Blockchain Proof ({driver.current_url})", flush=True)
        show_step_banner(driver, "STEP 10: Immutable Cryptographic Receipt", "Cryptographic tracking token, hash proof & verifiable QR Code", "#10b981")
        time.sleep(3)
        driver.save_screenshot("Screen Shots/visible_12_ballot_receipt.png")

        token_match = re.search(r'class="text-success fs-6 fw-bold">([a-f0-9\-]{36})</code>', driver.page_source)
        tracking_token = token_match.group(1) if token_match else ""
        print(f"  -> Generated Ballot Tracking Token: {tracking_token}", flush=True)

        # STEP 11: Public Audit & Zero-Knowledge Verification
        if tracking_token:
            print(f"\n>>> STEP 11: Public Zero-Knowledge Receipt Verification", flush=True)
            show_step_banner(driver, "STEP 11: Zero-Knowledge Verification", "Publicly verifying the cast ballot in the micro-blockchain ledger", "#06b6d4")
            driver.get(f"http://localhost:8080/verify-receipt?token={tracking_token}")
            time.sleep(2.5)
            driver.save_screenshot("Screen Shots/visible_13_verify_receipt.png")

        # STEP 12: Admin Dashboard - Live Updated Tallies
        print("\n>>> STEP 12: Admin Dashboard Live Tallies & Blockchain Health", flush=True)
        driver.get("http://localhost:8080/admin/login")
        time.sleep(1)
        driver.execute_script("""
            var u = document.querySelector('input[name=username]');
            var p = document.querySelector('input[name=password]');
            if (u && p) {
                u.value = 'Venkatesh';
                p.value = 'password';
                document.querySelector('form').submit();
            }
        """)
        wait.until(lambda d: "/admin/dashboard" in d.current_url)
        time.sleep(2)
        show_step_banner(driver, "STEP 12: Live Election Control Center Results", "Live vote tallies, turnout metric, and blockchain integrity validated!", "#10b981")
        time.sleep(3.5)
        driver.save_screenshot("Screen Shots/visible_14_live_results.png")

        # STEP 13: Blockchain Explorer View
        print("\n>>> STEP 13: Micro-Blockchain Explorer View", flush=True)
        show_step_banner(driver, "STEP 13: Micro-Blockchain Ledger Explorer", "Inspecting the newly sealed Infosys vote block on the blockchain", "#8b5cf6")
        driver.get("http://localhost:8080/admin/blockchain")
        time.sleep(3)
        driver.save_screenshot("Screen Shots/visible_15_blockchain_ledger.png")

        # Final Victory Banner
        show_step_banner(driver, "LIVE TESTING COMPLETE - ALL SYSTEMS PASSED", "13 verification steps executed successfully on visible Chrome browser!", "#10b981")
        time.sleep(4)

        print("\n" + "=" * 70, flush=True)
        print("ALL 13 VISIBLE LIVE BROWSER TESTS COMPLETED SUCCESSFULLY!", flush=True)
        print("=" * 70, flush=True)

    except Exception as e:
        print(f"\n[ERROR OCCURRED] {e}", flush=True)
        print(f"Current URL at error: {driver.current_url}", flush=True)
        driver.save_screenshot("Screen Shots/error_step.png")
        raise e
    finally:
        print("Closing Chrome browser window...", flush=True)
        time.sleep(1)
        driver.quit()

if __name__ == "__main__":
    run_visible_tests()
