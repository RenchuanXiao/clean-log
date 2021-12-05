import com.etix.BaseHttpController;
import com.etix.Contact;
import com.etix.ContactService;
import com.etix.DeliveryMethod;
import com.etix.DeliveryMethodService;
import com.etix.DeliveryMethodTypeService;
import com.etix.MailTransfer;
import com.etix.Order;
import com.etix.Organization;
import com.etix.OrganizationInfoEmbeddable;
import com.etix.OrganizationService;
import com.etix.PriceCode;
import com.etix.SellableItem;
import com.etix.TransactionLog;
import com.etix.Utilities;
import com.etix.accountManager.CustomerVoidTicketService;
import com.etix.crm.Subscription;
import com.etix.crm.SubscriptionService;
import com.etix.documentBuilder.Layout;
import com.etix.documentBuilder.TicketStock;
import com.etix.donation.DonationService;
import com.etix.exception.EntityNotFoundException;
import com.etix.exception.OverCapacityException;
import com.etix.log.CustomerProfileLog;
import com.etix.mail.EmailMessage;
import com.etix.payment.CurrencyService;
import com.etix.payment.exception.RefundAmountException;
import com.etix.sale.service.TransactionComponentDetailService;
import com.etix.security.Customer;
import com.etix.security.CustomerService;
import com.etix.security.CustomerSignInContext;
import com.etix.security.PasswordHelper;
import com.etix.security.VerifiedStatus;
import com.etix.ticketing.HttpServletAttribute;
import com.etix.ticketing.OrderDelivery;
import com.etix.ticketing.OrderDeliveryService;
import com.etix.ticketing.Package;
import com.etix.ticketing.PackageInformation;
import com.etix.ticketing.PackageInformationService;
import com.etix.ticketing.PackagePerformanceService;
import com.etix.ticketing.PackagePriceCodeService;
import com.etix.ticketing.Performance;
import com.etix.ticketing.PerformancePriceCode;
import com.etix.ticketing.PerformancePriceCodeService;
import com.etix.ticketing.PriceCodeTicketLimit;
import com.etix.ticketing.PrintOrder;
import com.etix.ticketing.Ticket;
import com.etix.ticketing.TicketCart;
import com.etix.ticketing.TicketOrder;
import com.etix.ticketing.TicketOrderService;
import com.etix.ticketing.TicketSale;
import com.etix.ticketing.TicketSeat;
import com.etix.ticketing.TransactionComponentDetail;
import com.etix.ticketing.accountManager.invoice.OrderInvoice;
import com.etix.ticketing.accountManager.invoice.OrderInvoiceService;
import com.etix.ticketing.accountManager.message.AccountManagerMessage;
import com.etix.ticketing.accountManager.message.AccountManagerMessageService;
import com.etix.ticketing.controller.MembershipTicketsResults;
import com.etix.ticketing.controller.SubscriptionEventSeriesRecord;
import com.etix.ticketing.controller.SubscriptionOrganizationGroupRecord;
import com.etix.ticketing.controller.SubscriptionPackageRecord;
import com.etix.ticketing.controller.SubscriptionPerformanceRecord;
import com.etix.ticketing.crm.MiscellaneousInfoService;
import com.etix.ticketing.crm.TicketLimit;
import com.etix.ticketing.exception.TicketLimitException;
import com.etix.util.Database;
import com.etix.util.Environment;
import com.etix.util.FunctionUtils;
import com.etix.util.Injector;
import com.etix.util.InternationalizationUtil;
import com.etix.util.JPAUtil;
import com.etix.util.Network;
import com.etix.util.StringUtil;
import com.etix.web.WebLocale;
import com.intellimark.ticketing.homePage.Pager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AccountManager extends DispatchAction {
    public static final Logger logger = Logger.getLogger("com.etix.ticketing.accountManager");

    public static final String CUSTOMER_FROM = "com.etix.ticketing.accountManager.AccountManager.session.key";
    public static final String CUSTOMER_ETIX = "etixSite";
    public static final String VIRTUAL_SHOPPING_CART = "virtualShoppingCart";

    public static final String NAVIGATION_MODULE_OVERVIEW = "overview";
    public static final String NAVIGATION_MODULE_MY_TICKETS = "myTickets";
    public static final String NAVIGATION_MODULE_MEMBERSHIP = "membership";
    public static final String NAVIGATION_MODULE_DONATIONS = "donations";
    public static final String NAVIGATION_MODULE_HELP = "help";
    public static final String NAVIGATION_MODULE_ACCOUNT = "account";

    /**
     * httpSession expired time in second is set to 15 minute
     */
    public static final int HTTPSESSION_EXPIRED_SECONDS = 15 * 60;

    public static final int HOME_PAGE_SIZE = 5;
    public static final int PAGE_SIZE = 24;

    /**
     * the action forward "changePassword"
     */
    public static final String CHANGE_PASSWORD = "changePassword";

    /**
     * the action forward "customerPrint"
     */
    public static final String CUSTOMER_PRINT = "customerPrint";

    /**
     * the action forward "recipientPrint"
     */
    public static final String RECIPIENT_PRINT = "recipientPrint";

    /**
     * the action forward "transferContact"
     */
    public static final String TRANSFER_CONTACT = "transferContact";
    public static final String TRANSFER_CONFIRE = "transferConfire";

    /**
     * the action forward "transferTicket"
     */
    public static final String TRANSFER_TICKET = "transferTicket";

    /**
     * the action forward "receiveTicket"
     */
    public static final String RECEIVE_TICKET = "receiveTicket";

    /**
     * the action forward "returnTicket"
     */
    public static final String RETURN_TICKET = "returnTicket";

    /**
     * the action forward "receiveTicketResults"
     */
    public static final String RECEIVE_TICKET_RESULTS = "receiveTicketResults";

    /**
     * the action forward "returnTicketResults"
     */
    public static final String RETURN_TICKET_RESULTS = "returnTicketResults";

    /**
     * the action forward "signIn"
     */
    public static final String SIGN_IN = "signIn";

    /**
     * the action forward "error"
     */
    public static final String ERROR = "error";

    /**
     * the object name used in dynamic page
     */
    public static final String TICKET_INFO_LIST = "ticketInfoList";
    public static final String ORDER_HISTORY_LIST = "orderHistoryList";
    public static final String INVOICE_ORDER_LIST = "invoiceOrderList";
    public static final String DONATE_RESULT = "donateResult";
    public static final String MY_DONATION_ORDERS = "myDonationOrders";
    public static final String PAST_DONATES = "pastDonates";
    public static final String ORDER_DETAIL_SHOW = "orderDetailShow";

    public static final String HISTORY_ALL = "ALL";
    public static final String HISTORY_UPCOMING = "UPCOMING";

    public static final String SORT_ASC = "ASC";
    public static final String SORT_DESC = "DESC";

    public static final String SHOW_ALL_TICKETS = "showAllTickets";

    private static final String KEY_ACCESS_DENIED = "accessDenied";

    private DateFormat timeformat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        request.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        response.setContentType("text/html;charset=UTF-8");
        setDateFormat(request);
        ActionForward errorForward = accessFunctionAuthorized(mapping, request);
        if (errorForward != null) {
            return errorForward;
        }
        return super.execute(mapping, form, request, response);
    }

    /**
     * Authorize a function in account manager whether can be accessed by the customer, only when the organization open the function
     * a customer can access it. Otherwise sent the customer to access denied page
     */
    private ActionForward accessFunctionAuthorized(ActionMapping mapping, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null) {
            return mapping.findForward(SIGN_IN);
        }

        Customer customer = context.getCustomer();
        if (context.getCustomer() == null) {
            if (Boolean.valueOf(request.getParameter("etixUI"))) {
                return mapping.findForward(SIGN_IN);
            }
            return redirect(context.getLoginPage());
        }
        String method = request.getParameter("method");
        OrganizationInfoEmbeddable orgInfo = OrganizationService.getInstance().findByPrimaryKeyJPA(customer.getOrganizationId()).getOrganizationInfo();
        if (method != null) {
            if (method.startsWith("exchange")) {
                if (orgInfo == null || !orgInfo.isTicketExchangable()) {
                    request.setAttribute("errorName", KEY_ACCESS_DENIED);
                    return mapping.findForward(ERROR);
                }
            } else if (method.toLowerCase().indexOf("donate") >= 0) {
                if (orgInfo == null || !orgInfo.isTicketDonatable()) {
                    request.setAttribute("errorName", KEY_ACCESS_DENIED);
                    return mapping.findForward(ERROR);
                }
            } else if (method.toLowerCase().indexOf("transfer") >= 0) {
                if (orgInfo == null || !orgInfo.isTicketTransferable()) {
                    request.setAttribute("errorName", KEY_ACCESS_DENIED);
                    return mapping.findForward(ERROR);
                }
            }
        }
        return null;
    }

    private void setDateFormat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null) {
            return;
        }
        String location = context.getLangCountry();
        if (location != null && location.indexOf("_") > 0) {
            String[] lc = location.split("_");
            this.timeformat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, new Locale(lc[0], lc[1]));
        }
    }

    public ActionForward changePasswordResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String oldPassword = request.getParameter("old_password");
        String password = request.getParameter("password").trim();

        VerifiedStatus result = null;
        if ((result = PasswordHelper.passwordRestrictionOnCustomer(password)) != VerifiedStatus.SUCCESS) {
            setMessage(request, result.getMessageKey());
            return mapping.findForward(CHANGE_PASSWORD);
        }

        String verifyPassword = request.getParameter("verify_password");
        String action = TransferAuthorization.AUTHORIZE_PASSWORD;
        try {
            // security checking
            TransferAuthorization.authorize(Customer.generatePasswordDigest(oldPassword), null, action, context.getCustomer().getCustomerID());
            TransferProcess transferProcess = new TransferProcess();
            if (password == null || password.trim().equalsIgnoreCase("") || verifyPassword == null
                    || verifyPassword.trim().equalsIgnoreCase("") || !password.equals(verifyPassword)) {
                setMessage(request, "errors.passwordNotMatch");
                return mapping.findForward(CHANGE_PASSWORD);
            }

            String url = "http://" + request.getServerName() + request.getContextPath()
                    + "/accountManager/signIn.jsp?orgId=" + context.getOrganization().getOrganizationID()
                    + getCobrandParam(context.getCobrand());
            if (!context.isShowNewUserLink()) {
                url += "&snul=n";
            }
            transferProcess.changePasswordProcess(password, context.getCustomer().getCustomerID(), url, context
                    .getBundle());
            setMessage(request, "online.signIn.editprofile.changepassword.successful");
            request.setAttribute("UPDATED_SEND_MAIL", context.getCustomer().getContact().getEmail());
            return mapping.findForward(CHANGE_PASSWORD);
        } catch (EntityNotFoundException ex) {
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            if ("This old password do not belong to this customer".equals(ex.getMessage())) {
                setMessage(request, "online.signIn.editprofile.changepassword.oldPassword.NotCorrect");
                return mapping.findForward(CHANGE_PASSWORD);
            } else {
                request.setAttribute("errorName", KEY_ACCESS_DENIED);
                return mapping.findForward(ERROR);
            }

        }
    }

    public ActionForward printTicket(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException, ServletException {
        return transferContact(mapping, form, request, response);
    }

    public ActionForward transferContact(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                         HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String submissionType = request.getParameter("submission_type");
        String orderId = request.getParameter("order_id");
        String[] selectedTickets = request.getParameterValues("ticket_id");
        if (selectedTickets == null) {
            selectedTickets = new String[0];
        }

        String action = TransferAuthorization.AUTHORIZE_ORDER_SELECTED_TICKETS;
        try {
         // security checking
            TransferAuthorization.authorize(orderId, selectedTickets, action, context.getCustomer().getCustomerID());

            if ((submissionType != null) && (submissionType.equals("Print"))) {

                TransferProcess transferProcess = new TransferProcess();
                transferProcess.printTicketProcess(selectedTickets);
                request.setAttribute(TransferProcess.PRINT_TICKET_PROCESS, transferProcess);

                return mapping.findForward(CUSTOMER_PRINT);

            } else if ((submissionType != null) && (submissionType.equals("Transfer")) || "confire".equals(request.getParameter("operate"))) {
                TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(Long.parseLong(orderId));
                // use transferableTickets to check the selected tickets whether can transfer
                ArrayList<Ticket> transferableTickets = ticketOrder.getTransferableTickets();
                ArrayList<Ticket> selectTransferTickets = new ArrayList<Ticket>();

                for (int i = 0; i < selectedTickets.length; i++) {
                    for (Ticket t : transferableTickets) {
                        if (selectedTickets[i].equals("" + t.getTicketID())) {
                            selectTransferTickets.add(t);
                        }
                    }
                }

                List<HashMap<String, String>> ticketsList = new ArrayList<HashMap<String, String>>();
                addItemToUpcomingList(ticketOrder.getCreationDate(), Long.parseLong(orderId), "transferable", selectTransferTickets, ticketsList, true);

                if (ticketsList.size() == 0) {
                    ticketsList = null;
                }

                request.setAttribute(TICKET_INFO_LIST, ticketsList);

                if ("confire".equals(request.getParameter("operate"))) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("firstName", request.getParameter("first_name"));
                    map.put("lastName", request.getParameter("last_name"));
                    map.put("email", request.getParameter("email").toLowerCase());
                    map.put("message", request.getParameter("message") == null ? "" : request.getParameter("message").trim());
                    map.put("days", request.getParameter("days"));

                    session.setAttribute("FriendContactInfo", map);
                    request.setAttribute("ticketCount", ticketsList.size());
                    return mapping.findForward(TRANSFER_CONFIRE);
                } else {
                    return mapping.findForward(TRANSFER_CONTACT);
                }
            } else {
                request.setAttribute("errorName", "NoSubmissionTypeSelected");
                return mapping.findForward(ERROR);
            }

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING, "EntityNotFoundException", ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "SQLException", ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING, "AccessControlException", ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward donateConfirm(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String orderId = request.getParameter("order_id");
        String[] selectedTickets = request.getParameterValues("ticket_id");
        if (selectedTickets == null) {
            selectedTickets = new String[0];
        }

        String action = TransferAuthorization.AUTHORIZE_ORDER_SELECTED_TICKETS;
        try {
            // security checking
            TransferAuthorization.authorize(orderId, selectedTickets, action, context.getCustomer().getCustomerID());

            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(Long.parseLong(orderId));
            // use transferableTickets to check the selected tickets whether can transfer
            ArrayList<Ticket> transferableTickets = ticketOrder.getTransferableTickets();
            ArrayList<Ticket> selectTransferTickets = new ArrayList<Ticket>();

            for (int i = 0; i < selectedTickets.length; i++) {
                for (Ticket t : transferableTickets) {
                    if (selectedTickets[i].equals("" + t.getTicketID())) {
                        selectTransferTickets.add(t);
                    }
                }
            }

            List<HashMap<String, String>> ticketsList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(ticketOrder.getCreationDate(), Long.parseLong(orderId), "transferable", selectTransferTickets, ticketsList, true);

            request.setAttribute("ticketCount", ticketsList.size());
            if (ticketsList.size() == 0) {
                ticketsList = null;
            }

            request.setAttribute(TICKET_INFO_LIST, ticketsList);
            return mapping.findForward("donateConfirm");

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    public ActionForward transferTicket(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        HashMap<String, String> contactInfo = (HashMap<String, String>) session.getAttribute("FriendContactInfo");

        String orderId = request.getParameter("order_id");
        String firstName = contactInfo.get("firstName");
        String lastName = contactInfo.get("lastName");
        String email = contactInfo.get("email");
        String message = contactInfo.get("message");
        String days = contactInfo.get("days");

        String[] selectedTickets = request.getParameterValues("ticket_id");
        if (selectedTickets == null) {
            selectedTickets = new String[0];
        }

        String action = TransferAuthorization.AUTHORIZE_ORDER_SELECTED_TICKETS;
        try {
            // security checking
            TransferAuthorization.authorize(orderId, selectedTickets, action, context.getCustomer().getCustomerID());
            // process transfer ticket
            TransferProcess transferProcess = new TransferProcess();
            if (selectedTickets.length == 0 || firstName.trim().equalsIgnoreCase("")
                    || lastName.trim().equalsIgnoreCase("") || email.trim().equalsIgnoreCase("")
                    || days.trim().equalsIgnoreCase("")) {
                response.getWriter().write("errors.noTicketsSelected");
                response.flushBuffer();
                response.getWriter().close();
                return null;
            }

            transferProcess.transferTicketProcess(selectedTickets, context.getCustomer().getCustomerID(), firstName,
                    lastName, email, message, days, context.getBundle(), context.getOrganization()
                                                                                .getOrganizationID());
            request.setAttribute(TransferProcess.TRANSFER_TICKET_PROCESS, transferProcess);
            //clear the FriendContactInfo in session
            session.removeAttribute("FriendContactInfo");

            return mapping.findForward(TRANSFER_TICKET);

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward receiveTicket(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String transferOrderId = request.getParameter("transfer_id");
        String action = TransferAuthorization.AUTHORIZE_TRANSFER_ORDER;
        // security checking
        try {
            TransferAuthorization.authorize(transferOrderId, null, action, context.getCustomer().getCustomerID());
            TransferProcess transferProcess = new TransferProcess();
            transferProcess.displayTransferableOrderProcess(transferOrderId);

            TransferOrder transferOrder = TransferOrder.findByPrimaryKey(transferOrderId);
            ArrayList<Ticket> transferableTickets = transferOrder.getTransferableTickets();

            List<HashMap<String, String>> upcomingList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(null, Long.parseLong(transferOrderId), "transferable", transferableTickets, upcomingList, true);

            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
            Customer sender = transferProcess.getCustomerRecipient();

            HashMap<String, String> map = new HashMap<String, String>();
            map.put("senderFullName", sender.getContact().getFirstName() + " " + sender.getContact().getLastName());
            map.put("senderEmail", sender.getContact().getEmail());
            map.put("senderMessage", transferOrder.getMessage());
            map.put("senderFullTime", formatter.format(transferOrder.getRequestDate().getTime()));
            map.put("expirationDate", formatter.format(transferOrder.getExpirationDate().getTime()));


            if (upcomingList.size() == 0) {
                upcomingList = null;
            }

            request.setAttribute("senderInformation", map);
            request.setAttribute(TICKET_INFO_LIST, upcomingList);

            return mapping.findForward(RECEIVE_TICKET);
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,"NumberFormatException",ex);
            request.setAttribute("errorName", ex.getMessage());
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward receiveTicketResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String transferOrderId = request.getParameter("transfer_id");
        String[] ticketIds = request.getParameterValues("ticket_id");
        String selectedIdStr = "";
        String rejectedIdStr = "";
        String[] selectedTickets = new String[0];
        String[] rejectedTickets = new String[0];
        if (ticketIds != null) {
            for (int i = 0; i < ticketIds.length; i++) {
                String option = request.getParameter("option_" + i);
                if ("1".equals(option)) {
                    if ("".equals(selectedIdStr)) {
                        selectedIdStr += ticketIds[i];
                    } else {
                        selectedIdStr += "," + ticketIds[i];
                    }
                } else {
                    if ("".equals(rejectedIdStr)) {
                        rejectedIdStr += ticketIds[i];
                    } else {
                        rejectedIdStr += "," + ticketIds[i];
                    }
                }
            }
            if (!"".equals(selectedIdStr)) {
                selectedTickets = selectedIdStr.split(",");
            }
            if (!"".equals(rejectedIdStr)) {
                rejectedTickets = rejectedIdStr.split(",");
            }
        }

        String action = TransferAuthorization.AUTHORIZE_TRANSFERABLE_ORDER_SELECTED_TICKETS;
        try {
            // security checking
            TransferAuthorization.authorize(transferOrderId, selectedTickets, action, context.getCustomer()
                    .getCustomerID());
            //check if there are tickets that can be transferred, fixed the issue of DEV-5435
            TransferOrder transferOrder = TransferOrder.findByPrimaryKey(transferOrderId);
            if(transferOrder.getTransferableTickets().isEmpty()) {
                request.setAttribute("errorName", "accountManager.noTicketsAvailableAcceptOrReject");
                return mapping.findForward(ERROR);
            }

            // process receive ticket
            TransferProcess transferProcess = new TransferProcess();
            transferProcess.receiveTicketProcess(transferOrderId, selectedTickets, context.getCustomer()
                    .getCustomerID(), context.getBundle());

            String orderId = "";
            ArrayList<Ticket> selectedTicketList = new ArrayList<Ticket>();
            for (String ticketId : selectedTickets) {
                Ticket ticket = Ticket.findByPrimaryKey(ticketId);
                selectedTicketList.add(ticket);

                if ("".equals(orderId)) {
                    orderId = String.valueOf(ticket.getOrder().getOrderID());
                    setPrintAtHomeAttribute(request, orderId);
                }
            }
            List<HashMap<String, String>> selectedTicketInfoList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(null, Long.parseLong(transferOrderId), "selectedTicket", selectedTicketList, selectedTicketInfoList, true);

            //check if show the column of "Add to Calendar and Share"
            boolean showAddToCalendar = false;
            for(HashMap<String, String> ticketInfoMap : selectedTicketInfoList) {
                Ticket ticket = Ticket.findByPrimaryKey(ticketInfoMap.get("ticketId"));
                if((ticket.getPackageID() == -1 && ticket.getPerformance().isPublicPerformance() || ticket.getPackageID() != -1)
                        && ticket.getVenue().getVenueInfo().isAddEventToCalendarOn()) {
                    showAddToCalendar = true;
                    break;
                }
            }

            //parse printable selected tickets
            parsePrintableTickets(request, selectedTicketInfoList);

            ArrayList<Ticket> rejectedTicketList = new ArrayList<Ticket>();
            for (String ticketId : rejectedTickets) {
                Ticket ticket = Ticket.findByPrimaryKey(ticketId);
                rejectedTicketList.add(ticket);
            }
            List<HashMap<String, String>> rejectedTicketInfoList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(null, Long.parseLong(transferOrderId), "rejectedTicket", rejectedTicketList, rejectedTicketInfoList, true);

            request.setAttribute("selectedTicketInfoList", selectedTicketInfoList);
            request.setAttribute("rejectedTicketInfoList", rejectedTicketInfoList);
            request.setAttribute("showAddToCalendar", showAddToCalendar);

            return mapping.findForward(RECEIVE_TICKET_RESULTS);

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward recipientPrint(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String transferOrderId = request.getParameter("transfer_id");
        String action = TransferAuthorization.AUTHORIZE_TRANSFER_ORDER;
        try {
            // security checking
            TransferAuthorization.authorize(transferOrderId, null, action, context.getCustomer().getCustomerID());

            TransferOrder transferOrder = TransferOrder.findByPrimaryKey(transferOrderId);
            ArrayList<Ticket> acceptedTickets = transferOrder.getTransferredTickets();

            List<HashMap<String, String>> upcomingList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(null, Long.parseLong(transferOrderId), "transferable", acceptedTickets, upcomingList, true);
            //parse printable tickets
            parsePrintableTickets(request, upcomingList);

            request.setAttribute(TICKET_INFO_LIST, upcomingList);
            return mapping.findForward(RECIPIENT_PRINT);

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    /**
     * Prepare which tickets can be printed, tickets list must be belong to the same order
     * @param preparePrintTickets
     */
    private void parsePrintableTickets(HttpServletRequest request, List<HashMap<String, String>> preparePrintTickets) throws SQLException, EntityNotFoundException {
        if(preparePrintTickets != null && !preparePrintTickets.isEmpty()) {
            //check the order's delivery method if is print at home
            Order order = Ticket.findByPrimaryKey(preparePrintTickets.get(0).get("ticketId")).getOrder();
            long deliveryMethodTypeId = order.getDeliveryMethod().getDeliveryMethodTypeId();
            if(!DeliveryMethodTypeService.isPrintAtHome(deliveryMethodTypeId)) {
                return;
            }

            List<String> printableTicketIds = new ArrayList<>();
            List<Ticket> printableTicketsList = new ArrayList<>();
            TicketStock ticketStock = PrintOrder.getTicketStock(order);
            for(HashMap<String, String> preparePrintTicketMap : preparePrintTickets) {
                Ticket ticket = Ticket.findByPrimaryKey(preparePrintTicketMap.get("ticketId"));
                //do not print/transfer performance/package tickets from a performance/package with print restriction
                if(ticket.getPackageID() == -1) { //performance ticket
                    if(ticket.getPerformance().isPerformanceTicketPrintRestricted()) {
                        continue;
                    }
                } else { // package ticket
                    if(ticket.getPackage().isPackageTicketPrintRestricted()) {
                        continue;
                    }
                    Layout packageLayout = ticket.getPackage().getTicketLayout(ticketStock.getStockID());
                    // no package specific layout, use each performance's layout, so will check the performance print restriction
                    if(packageLayout == null && ticket.getPerformance().isPerformanceTicketPrintRestricted()) {
                        continue;
                    }
                }
                printableTicketIds.add(String.valueOf(ticket.getTicketID()));
                printableTicketsList.add(ticket);
            }

            if(!printableTicketsList.isEmpty()) {
                List<OrderDelivery> orderDeliveryList = null;
                try {
                    orderDeliveryList = OrderDeliveryService.getOrderDeliveryListByOrderId(printableTicketsList.get(0).getOrder().getOrderID());
                }catch (EntityNotFoundException ex) {
                    //ignore
                    logger.log(Level.WARNING,"EntityNotFoundException",ex);
                }

                //if the order has delayed print of print at home
                if(orderDeliveryList != null && !orderDeliveryList.isEmpty()) {
                    printableTicketIds = PrintOrder.getOrderDeliveryPrintableTicketIdList(printableTicketsList.toArray(new Ticket[printableTicketsList.size()]), orderDeliveryList);
                    //key: ticketId; value: shipmentDate
                    Map<String, String> ticketDelayedPrintDateMap = new HashMap<>();
                    //key: eventId; value: shipmentDate
                    Map<Long, String> orderDeliveryDateMap = new HashMap<>();
                    for(OrderDelivery orderDelivery : orderDeliveryList) {
                        orderDeliveryDateMap.put(orderDelivery.getEventId(), orderDelivery.getShipmentDateString());
                    }
                    //get shipmentDate for delayed print tickets
                    for(Ticket ticket : printableTicketsList) {
                        if(!printableTicketIds.contains(String.valueOf(ticket.getTicketID()))) {
                            if(ticket.getPackageID() == -1) {
                                ticketDelayedPrintDateMap.put(String.valueOf(ticket.getTicketID()), orderDeliveryDateMap.get(ticket.getPerformanceID()));
                            } else {
                                ticketDelayedPrintDateMap.put(String.valueOf(ticket.getTicketID()), orderDeliveryDateMap.get(ticket.getPackageID()));
                            }
                        }
                    }
                    request.setAttribute("ticketDelayedPrintDateMap", ticketDelayedPrintDateMap);
                }
            }

            request.setAttribute("printableTicketIds", printableTicketIds);
            //this used for jstl fn:contains() method
            request.setAttribute("commaDelimitedPrintableTicketIds", StringUtil.toCommaDelimitedString(printableTicketIds.toArray(new String[printableTicketIds.size()])));
        }
    }

    public ActionForward recipientStartPrint(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String transferOrderId = request.getParameter("transfer_id");
        String[] selectedTickets = request.getParameterValues("ticket_id");
        if (selectedTickets == null) {
            selectedTickets = new String[0];
        }

        String action = TransferAuthorization.AUTHORIZE_TRANSFERRED_ORDER_SELECTED_TICKETS;
        try {
            // security checking
            TransferAuthorization.authorize(transferOrderId, selectedTickets, action, context.getCustomer()
                    .getCustomerID());
            TransferProcess transferProcess = new TransferProcess();
            if (selectedTickets.length == 0) {
                response.getWriter().write("errors.noTicketsSelected");
                response.flushBuffer();
                response.getWriter().close();
                return null;
            }
            transferProcess.printTicketProcess(selectedTickets);
            request.setAttribute(TransferProcess.PRINT_TICKET_PROCESS, transferProcess);
            return mapping.findForward(CUSTOMER_PRINT);

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward returnTicket(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String transferOrderId = request.getParameter("transfer_id");
        String action = TransferAuthorization.AUTHORIZE_TRANSFER_ORDER;
        try {
            // security checking
            TransferAuthorization.authorize(transferOrderId, null, action, context.getCustomer().getCustomerID());

            TransferOrder transferOrder = TransferOrder.findByPrimaryKey(transferOrderId);
            ArrayList<Ticket> acceptedTickets = transferOrder.getTransferredTickets();

            List<HashMap<String, String>> upcomingList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(null, Long.parseLong(transferOrderId), "transferable", acceptedTickets, upcomingList, true);

            if (upcomingList.size() == 0) {
                upcomingList = null;
            }
            request.setAttribute(TICKET_INFO_LIST, upcomingList);
            return mapping.findForward(RETURN_TICKET);

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward returnTicketResults(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String transferOrderId = request.getParameter("transfer_id");
        String[] selectedTickets = request.getParameterValues("ticket_id");
        if (selectedTickets == null) {
            selectedTickets = new String[0];
        }

        String action = TransferAuthorization.AUTHORIZE_TRANSFERRED_ORDER_SELECTED_TICKETS;
        try {
            // security checking
            TransferAuthorization.authorize(transferOrderId, selectedTickets, action, context.getCustomer()
                    .getCustomerID());
            // process return ticket
            TransferProcess transferProcess = new TransferProcess();
            if (selectedTickets.length == 0) {
                response.getWriter().write("errors.noTicketsSelected");
                response.flushBuffer();
                response.getWriter().close();
                return null;
            }

            transferProcess.returnTicketProcess(transferOrderId, selectedTickets,
                    context.getCustomer().getCustomerID(), context.getBundle());
            request.setAttribute(TransferProcess.RETURN_TICKET_PROCESS, transferProcess);
            return mapping.findForward(RETURN_TICKET_RESULTS);
        } catch (EntityNotFoundException ex) {
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward receiveTransferOrder(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                              HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        Customer customer = context.getCustomer();
        String customerName = customer.getName();

        List<HashMap<String, String>> transferOrdersList = new ArrayList<HashMap<String, String>>();

        try {
            ArrayList<?> transferableOrders = TransferOrder.getTransferableOrders(customerName, customer.getOrganizationId());
            DateFormat timeformat = DateFormat.getDateInstance(DateFormat.FULL);
            long tempTransferId = Long.MIN_VALUE;
            for (int i = 0; i < transferableOrders.size(); i++) {
                TransferOrder transfer = (TransferOrder) transferableOrders.get(i);
                long transferId = transfer.getTransferId();
                ArrayList<?> transferableTickets = transfer.getTransferableTickets();

                String temp = "";
                String addTransferId = "";
                String addAcceptedTicketsSize = "";
                for (int j = 0; j < transferableTickets.size(); j++) {
                    Ticket ticket = (Ticket) transferableTickets.get(j);
                    long perfId = ticket.getPerformanceID();
                    Performance perf = Performance.findByPrimaryKey(perfId);
                    String perfName = perf.getName();
                    if (!perfName.equalsIgnoreCase(temp)) {

                        if (transferId != tempTransferId) {
                            addTransferId = "" + transferId;
                            addAcceptedTicketsSize = "" + transferableTickets.size();
                        } else {
                            addTransferId = "";
                            addAcceptedTicketsSize = "";
                        }

                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("transferId", addTransferId);
                        map.put("acceptedTicketsSize", addAcceptedTicketsSize);
                        timeformat.setTimeZone(perf.getVenue().getTz());
                        map.put("performanceTime", timeformat.format(perf.getDateTimeWithTimeZone().getTime()));
                        map.put("performanceName", perf.getName());
                        transferOrdersList.add(map);

                        tempTransferId = transferId;
                    }
                    temp = perfName;
                }
            }

            if (transferOrdersList.size() == 0) {
                transferOrdersList = null;
            }

            request.setAttribute("transferOrdersList", transferOrdersList);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("receiveTransferOrder");
    }

    public ActionForward returnTransferOrder(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        Customer customer = context.getCustomer();
        String customerName = customer.getName();

        List<HashMap<String, String>> receiveTicketsList = new ArrayList<>();

        try {
            ArrayList<TransferOrder> transferredOrders = TransferOrder.getTransferredOrders(customerName, customer.getOrganizationId());
            DateFormat timeformat = DateFormat.getDateInstance(DateFormat.FULL);
            long tempTransferId = Long.MIN_VALUE;
            for (int i = 0; i < transferredOrders.size(); i++) {
                TransferOrder transfer = transferredOrders.get(i);
                long transferId = transfer.getTransferId();
                ArrayList<Ticket> acceptedTickets = transfer.getTransferredTickets();
                //check accepted tickets if can be printed, these tickets of same transfer order should belong to the same order
                boolean printable = getPrintableTickets(acceptedTickets).size() > 0;

                String temp = "";
                String addTransferId = "";
                String addAcceptedTicketsSize = "";
                for (int j = 0; j < acceptedTickets.size(); j++) {
                    Ticket ticket = acceptedTickets.get(j);
                    Performance perf = ticket.getPerformance();
                    String perfName = perf.getName();
                    if (!perfName.equalsIgnoreCase(temp)) {

                        if (transferId != tempTransferId) {
                            addTransferId = "" + transferId;
                            addAcceptedTicketsSize = "" + acceptedTickets.size();
                        } else {
                            addTransferId = "";
                            addAcceptedTicketsSize = "";
                        }

                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("transferId", addTransferId);
                        map.put("acceptedTicketsSize", addAcceptedTicketsSize);
                        timeformat.setTimeZone(perf.getVenue().getTz());
                        map.put("performanceTime", timeformat.format(perf.getDateTimeWithTimeZone().getTime()));
                        map.put("performanceName", perf.getName());
                        map.put("printAtHome", Boolean.toString(printable));
                        receiveTicketsList.add(map);

                        tempTransferId = transferId;
                    }
                    temp = perfName;
                }
            }

            request.setAttribute("receiveTicketsList", receiveTicketsList);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING,"EntityNotFoundException",e);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("returnTransferOrder");
    }

    public ActionForward forwardHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            Customer customer = context.getCustomer();
            long orderId = Long.parseLong(request.getParameter("order_id"));
            //check the order whether belong to the customer
            TransferAuthorization.authorizeOrder(String.valueOf(orderId), customer.getCustomerID());
            List<TransferOrder> forwardHistoryList = TransferOrder.getForwardHistoryOfOrder(orderId);
            request.setAttribute("forwardHistoryList", forwardHistoryList);
            request.setAttribute("orderId", orderId);
            return mapping.findForward("forwardHistory");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "NumberFormatException", e);
            request.setAttribute("errorName", e.getMessage());
            return mapping.findForward(ERROR);
        } catch (AccessControlException e) {
            logger.log(Level.WARNING, "AccessControlException", e);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward forwardedOrderDetails(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            Customer customer = context.getCustomer();
            long transferId = Long.parseLong(request.getParameter("transfer_id"));
            TransferOrder transferOrder = TransferOrder.findByPrimaryKey(transferId);
            List<Ticket> ticketList = transferOrder.getTransferTickets();
            if (!ticketList.isEmpty()) {
                //check the order whether belong to the customer
                TransferAuthorization.authorizeOrder(String.valueOf(ticketList.get(0).getOrder().getOrderID()), customer.getCustomerID());
            }
            List<Map<String, String>> forwardedTicketsList = new ArrayList<>();
            for (Ticket ticket : ticketList) {
                Performance perf = ticket.getPerformance();
                HashMap<String, String> ticketInfoMap = new HashMap<>();
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                format.setTimeZone(perf.getVenue().getTz());
                ticketInfoMap.put("performanceTime", format.format(perf.getDateTimeWithTimeZone().getTime()));
                ticketInfoMap.put("performanceName", perf.getName());
                ticketInfoMap.put("venueName", perf.getVenue().getName());
                ticketInfoMap.put("seat", ticket.getFirstSectionName() + "&bull;" +
                        ticket.getFirstRowName() + "&bull;" +
                        ticket.getFirstSeatName());
                TransferTicket transferTicket = TransferTicket.findByTransferIdAndTicketId(transferId, ticket.getTicketID());
                ticketInfoMap.put("statusKey", TransferTicket.getTicketForwardStatusKey(transferTicket));
                forwardedTicketsList.add(ticketInfoMap);
            }
            request.setAttribute("forwardedTicketsList", forwardedTicketsList);
            request.setAttribute("transferOrder", transferOrder);
            return mapping.findForward("forwardedOrderDetails");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,"NumberFormatException",ex);
            request.setAttribute("errorName", ex.getMessage());
            return mapping.findForward(ERROR);
        } catch (AccessControlException e) {
            logger.log(Level.WARNING,"AccessControlException",e);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    /**
     * get printable tickets by print at home, tickets list must be belong to the same order
     */
    public static List<Ticket> getPrintableTickets(List<Ticket> tickets) {
        List<Ticket> printableTickets = new ArrayList<>();
        if(tickets == null || tickets.isEmpty()) {
            return printableTickets;
        }
        long deliveryMethodTypeId = tickets.get(0).getOrder().getDeliveryMethod().getDeliveryMethodTypeId();
        if(!DeliveryMethodTypeService.isPrintAtHome(deliveryMethodTypeId)) {
            return printableTickets;
        }

        TicketStock ticketStock = PrintOrder.getTicketStock(tickets.get(0).getOrder());
        for(Ticket ticket : tickets) {
            //do not print performance/package tickets from a performance/package with print restriction
            if(ticket.getPackageID() == -1) { //performance ticket
                if(ticket.getPerformance().isPerformanceTicketPrintRestricted()) {
                    continue;
                }
            } else { // package ticket
                if(ticket.getPackage().isPackageTicketPrintRestricted()) {
                    continue;
                }
                Layout packageLayout = null;
                try {
                    packageLayout = ticket.getPackage().getTicketLayout(ticketStock.getStockID());
                } catch (Exception e) {
                    logger.info("get package ticket layout has issue, packageId:" + ticket.getPackageID() + " ,stockID:" + ticketStock.getStockID());
                }
                // no package specific layout, use each performance's layout, so will check the performance print restriction
                if(packageLayout == null && ticket.getPerformance().isPerformanceTicketPrintRestricted()) {
                    continue;
                }
            }
            printableTickets.add(ticket);
        }

        if(!printableTickets.isEmpty()) {
            List<OrderDelivery> orderDeliveryList = null;
            try {
                orderDeliveryList = OrderDeliveryService.getOrderDeliveryListByOrderId(printableTickets.get(0).getOrder().getOrderID());
            }catch (EntityNotFoundException ex) {
                logger.log(Level.WARNING,"EntityNotFoundException",ex);
                //ignore
            }
            //if the order has delayed print of print at home
            if(orderDeliveryList != null && !orderDeliveryList.isEmpty()) {
                List<String> printableTicketIds = PrintOrder.getOrderDeliveryPrintableTicketIdList(printableTickets.toArray(new Ticket[printableTickets.size()]), orderDeliveryList);
                List<Ticket> printableDelayedTickets = new ArrayList<>();
                for(Ticket ticket : printableTickets) {
                    if(printableTicketIds.contains(String.valueOf(ticket.getTicketID()))) {
                        printableDelayedTickets.add(ticket);
                    }
                }
                return printableDelayedTickets;
            }
        }

        return printableTickets;
    }

    private String getCobrandParam(String cobrand) {
        String cobrandParam = "&cobrand=" + cobrand;
        if (cobrand == null || BaseHttpController.DEFAULT_ONLINE_BRAND_NAME.equals(cobrand) || cobrand.trim().equalsIgnoreCase("")) {
            cobrandParam = "";
        }
        return cobrandParam;
    }

    public ActionForward signOut(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            CustomerSignInContext context = (CustomerSignInContext) session
                    .getAttribute(CustomerSignInContext.CONTEXT_OBJ);
            if (context == null)
                return mapping.findForward(SIGN_IN);
            if (context.getCustomer() == null)
                return redirect(context.getLoginPage());

            /*Cobrand cobrand = CobrandHelper.getCobrandFromSession(request);
            ActionForward af = new ActionForward(mapping.findForward("signOut"));
            StringBuilder sb = new StringBuilder().append(af.getPath()).append("?language_country=").append(context.getLangCountry());
            
            if(cobrand != null){
            	sb.append(getCobrandParam(cobrand.getName()));
            }
            sb.append("&organization_id=").append(context.getOrganization().getOrganizationID());
            if (!context.isShowNewUserLink()) {
                sb.append("&snul=n");
            }

            sb.append("&loginPage=" + context.getLoginPage());

            af.setPath(sb.toString());*/
            //session.invalidate();
            @SuppressWarnings("unchecked")
            Enumeration<String> em = session.getAttributeNames();
            String key = "";
            while (em.hasMoreElements()) {
                key = em.nextElement();
                if (!key.equals(CUSTOMER_FROM)) {
                    session.removeAttribute(key);
                }
            }

        }

        return mapping.findForward(SIGN_IN);
    }

    private ActionForward redirect(String path) {
        ActionForward redirect = new ActionForward();
        redirect.setPath(path);
        redirect.setRedirect(true);
        return redirect;
    }

    public ActionForward customerWelcome(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                         HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());
        session.setAttribute(Customer.ONLINE_PROFILE, customer);
        try {
            if((request.getParameter("upcomingPageNumber") == null || session.getAttribute("homeUpcomingTicketPager") == null) &&
                    (request.getParameter("invoicePageNumber") == null || session.getAttribute("homeInvoiceOrderPager") == null) &&
                    (request.getParameter("historyPageNumber") == null) || session.getAttribute("homeHistoryOrderPager") == null) {
                List<Customer> customers = new ArrayList<Customer>();
                if (customer.isSystemCustomer()) {
                    customers.addAll(CustomerService.getOrganizationCustomers(customer.getCustomerID()));
                } else {
                    customers.add(customer);
                }
                List<HashMap<String, String>> upcomingList = new ArrayList<HashMap<String, String>>();
                List<HashMap<String, String>> orderList = new ArrayList<HashMap<String, String>>();
                List<PaymentInvoiceVO> invoiceOrderList = new ArrayList<PaymentInvoiceVO>();
                ArrayList<TransferOrder> transferableOrders = new ArrayList<TransferOrder>();
                ArrayList<TransferOrder> transferredOrders = new ArrayList<TransferOrder>();
                for (Customer cust : customers) {
                    upcomingList.addAll(getUpcomingTicket(cust, null, false, true, true));
                    orderList.addAll(getOrderHistory(cust, HISTORY_ALL, 4));
                    invoiceOrderList.addAll(CustomerInvoiceService.getAllPaymentPlan(cust.getCustomerID()));
                    transferableOrders.addAll(TransferOrder.getTransferableOrders(cust.getName(), cust.getOrganizationId()));
                    transferredOrders.addAll(TransferOrder.getTransferredOrders(cust.getName(), cust.getOrganizationId()));
                }

                if ((orderList == null || orderList.size() == 0) && transferableOrders.size() > 0) {
                    return redirect("accountManager.do?method=receiveTicket&transfer_id=" + ((TransferOrder) transferableOrders.get(0)).getTransferId());
                }

                List<AccountManagerMessage> messages = AccountManagerMessageService.getInstance().findDisplayOnlineMessages(customer.getOrganizationId());

                session.setAttribute("transferableOrders", transferableOrders);
                session.setAttribute("transferredOrders", transferredOrders);
                session.setAttribute("messageList", messages);

                session.setAttribute("homeUpcomingTicketPager", Pager.create(HOME_PAGE_SIZE, upcomingList));
                session.setAttribute("homeInvoiceOrderPager", Pager.create(HOME_PAGE_SIZE, invoiceOrderList));

                List<HashMap<String, String>> filteredOrderList = orderList.stream().filter(AccountManager::isPackageAnnounced).collect(Collectors.toList());
                LinkedHashSet<String> orderIdsSet = getOrderIdsSet(filteredOrderList);
                session.setAttribute("homeFilteredHistoryOrderList", filteredOrderList);
                session.setAttribute("homeHistoryOrderPager", Pager.create(HOME_PAGE_SIZE, new ArrayList<>(orderIdsSet)));
            }

            Pager homeUpcomingTicketPager = (Pager) session.getAttribute("homeUpcomingTicketPager");
            if(StringUtils.isNotBlank(request.getParameter("upcomingPageNumber"))) {
                homeUpcomingTicketPager.setPageNumber(Integer.parseInt(request.getParameter("upcomingPageNumber")));
            }
            Pager homeInvoiceOrderPager = (Pager) session.getAttribute("homeInvoiceOrderPager");
            if(StringUtils.isNotBlank(request.getParameter("invoicePageNumber"))) {
                homeInvoiceOrderPager.setPageNumber(Integer.parseInt(request.getParameter("invoicePageNumber")));
            }
            Pager homeHistoryOrderPager = (Pager) session.getAttribute("homeHistoryOrderPager");
            if(StringUtils.isNotBlank(request.getParameter("historyPageNumber"))) {
                homeHistoryOrderPager.setPageNumber(Integer.parseInt(request.getParameter("historyPageNumber")));
            }

            request.setAttribute(TICKET_INFO_LIST, homeUpcomingTicketPager != null ? homeUpcomingTicketPager.getSubItems() : null);
            request.setAttribute(INVOICE_ORDER_LIST, homeInvoiceOrderPager != null ? homeInvoiceOrderPager.getSubItems() : null);

            if(session.getAttribute("homeFilteredHistoryOrderList") != null) {
                List<HashMap<String, String>> filteredOrderList = (List<HashMap<String, String>> )session.getAttribute("homeFilteredHistoryOrderList");
                request.setAttribute(ORDER_HISTORY_LIST, filteredOrderList.stream().filter(e -> homeHistoryOrderPager.getSubItems().contains(e.get("orderId"))).collect(Collectors.toList()));
            }

        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "SQLException");
            return mapping.findForward(ERROR);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            request.setAttribute("errorName", "error");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("customerWelcome");
    }

    public ActionForward invoiceReview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> cstOrders = CustomerService.getOrders(customer.getUserID());
            long orderId = Long.parseLong(request.getParameter("order_id"));
            TicketOrder order = TicketOrder.findByPrimaryKey(orderId);
            if (cstOrders.contains(orderId)) {
                if (order.isVoided()) {
                    request.setAttribute("errorName", "reservationOrderPayment.error.orderVoid");
                    return mapping.findForward(ERROR);
                } else {
                    PaymentInvoiceVO invoiceOrder = CustomerInvoiceService.getOneOrderPaymentPlan(orderId);
                    request.setAttribute("invoiceOrder", invoiceOrder);
                    request.setAttribute("orderMiscellaneousInfoTitle", MiscellaneousInfoService.MISCELLANEOUS_INFO_TITLE);
                    request.setAttribute("orderMiscellaneousInfo", MiscellaneousInfoService.getInstance().buildDisplayableOrderMiscellaneousInfo(order, context.getBundle().getLocale()));
                }
            } else {
                request.setAttribute("errorName", "reservationOrderPayment.error.orderNotExist");
                return mapping.findForward(ERROR);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            request.setAttribute("errorName", "reservationOrderPayment.error.orderNotExist");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,"NumberFormatException",ex);
            request.setAttribute("errorName", ex.getMessage());
            return mapping.findForward(ERROR);
        }

        //return mapping.findForward("invoiceReview");
        return mapping.findForward("selectPaymentProcessor");
    }

    public ActionForward payment(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        //remove the virtual shopping cart when payment
        session.removeAttribute(VIRTUAL_SHOPPING_CART);
        try {
            List<Long> cstOrders = CustomerService.getOrders(customer.getUserID());
            long orderId = Long.parseLong(request.getParameter("order_id"));
            TicketOrder order = TicketOrder.findByPrimaryKey(orderId);
            if (cstOrders.contains(orderId)) {
                if (order.isVoided()) {
                    request.setAttribute("errorName", "reservationOrderPayment.error.orderVoid");
                    return mapping.findForward(ERROR);
                } else {
                    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, context.getBundle().getLocale());
                    List<Object[]> membershipRequiredTickets = new ArrayList<>();
                    Ticket ticket;
                    SellableItem[] items = order.getSellableItems();
                    Set<Long> fullPackageTicketIdSets = new HashSet<Long>();
                    for (SellableItem item : items) {
                        ticket = (Ticket) item;
                        if (SellableItem.STATUS_VOID.equalsIgnoreCase(ticket.getStatus())) {
                            continue;
                        }
                        if (fullPackageTicketIdSets.contains(ticket.getPackageTicketID())) {
                            continue;
                        }
                        boolean isTicketLevelLimit = ticket.getVenue().getVenueInfo().getMembershipValidation() == Subscription.TICKET_LEVEL;
                        if (isTicketLevelLimit) {
                            boolean hasMemberTicketLimitOnPriceCode = false;
                            PriceCodeTicketLimit priceCodeLimit = null;
                            if (ticket.getPackageID() > 0) { //package sale
                                priceCodeLimit = PackagePriceCodeService.findByPackagePriceCode(ticket.getPackageID(), ticket.getPriceCodeID());
                            } else { //performance sale
                                priceCodeLimit = PerformancePriceCodeService.findByPerformancePriceCode(ticket.getPerformanceID(), ticket.getPriceCodeID());
                            }
                            if (priceCodeLimit.isThereAnyTicketLimit()) {
                                hasMemberTicketLimitOnPriceCode = true;
                            }
                            //if the ticket has ticket limit and not inputed membership when reserving ticket
                            if (hasMemberTicketLimitOnPriceCode && ticket.getSubscriptionId() == -1) {
                                String activityName = ticket.getPerformance().getName();
                                String displayDate = dateFormat.format(ticket.getPerformance().getDate());
                                double displayPrice = ticket.getPrice();
                                double displayFee = ticket.getFeePaid();
                                double displayTotal = ticket.getPriceAndFeePaid();
                                if (ticket.getPackageID() > 0) {
                                    //only ask the customer enter the membership ID one time for full package seats
                                    com.etix.ticketing.Package pkg = com.etix.ticketing.Package.findByPrimaryKey(ticket.getPackageID());
                                    if (!pkg.isMiniPackage()) {
                                        fullPackageTicketIdSets.add(ticket.getPackageTicketID());
                                        activityName = ticket.getPackageName();
                                        displayDate = dateFormat.format(pkg.getDate());
                                        for (SellableItem t : items) {
                                            Ticket tix = (Ticket) t;
                                            if (tix.getTicketID() != ticket.getTicketID() && tix.getPackageTicketID() == ticket.getPackageTicketID()) {
                                                displayPrice += tix.getPrice();
                                                displayFee += tix.getFeePaid();
                                                displayTotal += tix.getPriceAndFeePaid();
                                            }
                                        }
                                    }
                                }

                                membershipRequiredTickets.add(new Object[]{Utilities.escapeSingleQuote(activityName),//0
                                        displayDate,//1
                                        ticket.getSectionNamesInHtmlStyle(),//2
                                        ticket.getRowNamesInHtmlStyle(),//3
                                        ticket.getSeatNamesInHtmlStyle(),//4
                                        ticket.getIsoCurrency().getIsoCode(),//5
                                        displayPrice,//6
                                        displayFee,//7
                                        displayTotal,//8
                                        ticket.getPriceCodeName(),//9
                                        ticket.getTicketID(),//10
                                });
                            }
                        }
                    }

                    if (membershipRequiredTickets.isEmpty()) {
                        return invoiceReview(mapping, form, request, response);
                    } else {
                        request.setAttribute("orderID", orderId);
                        request.setAttribute("membershipRequiredTickets", membershipRequiredTickets);
                        return mapping.findForward("checkMembership");
                    }
                }
            } else {
                request.setAttribute("errorName", "reservationOrderPayment.error.orderNotExist");
                return mapping.findForward(ERROR);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            request.setAttribute("errorName", "reservationOrderPayment.error.orderNotExist");
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward checkMembershipRequired(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                                 HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> cstOrders = CustomerService.getOrders(customer.getUserID());
            long orderId = Long.parseLong(request.getParameter("order_id"));
            TicketOrder order = TicketOrder.findByPrimaryKey(orderId);
            if (cstOrders.contains(orderId)) {
                if (order.isVoided()) {
                    request.setAttribute("errorName", "reservationOrderPayment.error.orderVoid");
                    return mapping.findForward(ERROR);
                } else {
                    //key:packageId + "_" + priceCodeId
                    Map<String, ArrayList<TicketSeat>> packageTicketSeats = new HashMap<>();
                    Map<String, String> checkInputMember = new HashMap<>();
                    //virtual shopping cart to check the membership required
                    TicketSale ticketSale = new TicketSale(order.getSalesChannelID());
                    ticketSale.setSeller(order.getSeller());
                    ticketSale.setExternalAttribute(new HttpServletAttribute(request.getSession(false)));
                    TicketCart cart = ticketSale.getTicketCart();

                    Ticket ticket;
                    SellableItem[] items = order.getSellableItems();
                    Performance currentPerformance = null;
                    for (SellableItem item : items) {
                        ticket = (Ticket) item;
                        if (currentPerformance == null) {
                            currentPerformance = ticket.getPerformance();
                        }
                        boolean isTicketLevelLimit = ticket.getVenue().getVenueInfo().getMembershipValidation() == Subscription.TICKET_LEVEL;
                        if (isTicketLevelLimit) {
                            boolean hasMemberTicketLimitOnPriceCode = false;
                            PriceCodeTicketLimit priceCodeLimit = null;
                            if (ticket.getPackageID() > 0) { //package sale
                                priceCodeLimit = (PriceCodeTicketLimit) PackagePriceCodeService.findByPackagePriceCode(ticket.getPackageID(), ticket.getPriceCodeID());
                            } else { //performance sale
                                priceCodeLimit = (PriceCodeTicketLimit) PerformancePriceCodeService.findByPerformancePriceCode(ticket.getPerformanceID(), ticket.getPriceCodeID());
                            }
                            if (priceCodeLimit.isThereAnyTicketLimit()) {
                                hasMemberTicketLimitOnPriceCode = true;
                            }
                            //if the ticket has ticket limit and not inputed membership when reserving ticket
                            if (hasMemberTicketLimitOnPriceCode && ticket.getSubscriptionId() == -1) {
                                String inputMemberId = request.getParameter("inputMemberId_" + ticket.getTicketID());
                                if (inputMemberId != null && !"".equals(inputMemberId.trim())) {
                                    checkInputMember.put(Long.toString(ticket.getTicketSeat().getTicketSeatId()), inputMemberId);
                                }

                                if (ticket.getPackageID() > 0) {
                                    // select key to only get one record for each package+price code+package ticket id combination
                                    String key = ticket.getPackageID() + "_" + ticket.getPriceCodeID() + "_" + ticket.getPackageTicketID();
                                    if (packageTicketSeats.get(key) == null) {
                                        ArrayList<TicketSeat> ticketSeats = new ArrayList<>();
                                        ticketSeats.add(ticket.getTicketSeat());
                                        packageTicketSeats.put(key, ticketSeats);
                                    } else {
                                        packageTicketSeats.get(key).add(ticket.getTicketSeat());
                                    }
                                } else {
                                    List<PerformancePriceCode> priceCodes = PerformancePriceCodeService.getPerformancePublicAndPrivatePriceCodes(ticket.getPerformance().getPerformanceID());
                                    cart.addPerformanceSeat(ticket.getTicketSeat(), Long.toString(ticket.getPriceCodeID()), priceCodes.toArray(new PerformancePriceCode[priceCodes.size()]), null);
                                }
                            }
                        }
                    }

                    for (String key : packageTicketSeats.keySet()) {
                        String[] ids = key.split("_");
                        cart.addPackageSeat(com.etix.ticketing.Package.findByPrimaryKey(ids[0]), packageTicketSeats.get(key), ids[1], com.etix.ticketing.Package.getPackagePriceCodes(Long.parseLong(ids[0]), true), null);
                    }

                    ticketSale.setCurrentPerformance(currentPerformance);
                    //check membership
                    Map<String, Exception> memberException = ticketSale.applySubscriptionToSeats(checkInputMember);
                    if (!memberException.isEmpty()) {
                        boolean isOverLimit = false;
                        for (String exceptionKey : memberException.keySet()) {
                            if (memberException.get(exceptionKey) instanceof TicketLimitException) {
                                isOverLimit = true;
                                break;
                            }
                        }
                        if (isOverLimit) {
                            request.setAttribute("errorName", "error.memberOverLimit");
                        } else {
                            request.setAttribute("errorName", "error.memberInvalid");
                        }
                        return mapping.findForward(ERROR);
                    } else {
                        try {
                            ticketSale.checkEligibleOfMemberLimit();
                        } catch (TicketLimitException e) {
                            request.setAttribute("errorName", e.getMessageKey());
                            return mapping.findForward(ERROR);
                        }
                    }

                    session.setAttribute(VIRTUAL_SHOPPING_CART, cart);
                    return invoiceReview(mapping, form, request, response);
                }
            } else {
                request.setAttribute("errorName", "reservationOrderPayment.error.orderNotExist");
                return mapping.findForward(ERROR);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            request.setAttribute("errorName", "reservationOrderPayment.error.orderNotExist");
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward paymentHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> cstOrders = CustomerService.getOrders(customer.getUserID());
            long orderId = Long.parseLong(request.getParameter("order_id"));

            if (!cstOrders.contains(orderId)) {
                return redirect(context.getLoginPage());
            }

			/*List<Long> findOrderIds = new ArrayList<Long>();
			findOrderIds.add(orderId);
			List<HashMap<String, String>> upcomingList = getUpcomingTicket(context, findOrderIds, true);
			request.setAttribute(TICKET_INFO_LIST, upcomingList);*/

            TicketOrder order = TicketOrder.findByPrimaryKey(orderId);

            String isoCode = CurrencyService.findByPrimaryKey(order.getCurrencyID()).getIsoCode();
            ArrayList<TransactionLog> list = order.getTransactions();
            if ("reserve".equals(list.get(0).getAction())) {
                request.setAttribute("isReserveOrder", true);
            }
            Collections.reverse(list);
            List<Map<Object, Object>> transactionHistorys = TicketOrderService.getAllTransactionHistory(orderId);
            request.setAttribute("order", order);
            request.setAttribute("isoCode", isoCode);
            request.setAttribute("transactionHistorys", transactionHistorys);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,"NumberFormatException",ex);
            request.setAttribute("errorName", ex.getMessage());
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("invoicePaymentHistory");
    }

    public ActionForward invoiceHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                        HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> cstOrders = CustomerService.getOrders(customer.getUserID());
            long orderId = Long.parseLong(request.getParameter("order_id"));

            if (!cstOrders.contains(orderId)) {
                return redirect(context.getLoginPage());
            }

            OrderInvoiceService oiService = OrderInvoiceService.getInstance();
            //try to create a new invoice record
            //oiService.createOrderInvoice(orderId);

            List<OrderInvoice> oiList = oiService.findAllByOrderId(orderId);
            Collections.reverse(oiList);
            if (oiList.size() == 0) {
                try {
                    oiList.add(oiService.getTempOrderInvoice(orderId));
                } catch (Exception e) {
                }
            }

            if (oiList.size() == 0) {
                oiList = null;
            }

            TicketOrder order = TicketOrder.findByPrimaryKey(orderId);
            String isoCode = CurrencyService.findByPrimaryKey(order.getCurrencyID()).getIsoCode();

            request.setAttribute("order", order);
            request.setAttribute("isoCode", isoCode);
            request.setAttribute("OrderInvoiceList", oiList);

        } catch (AccessControlException ex) {
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,"NumberFormatException",ex);
            request.setAttribute("errorName", ex.getMessage());
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (Exception ex) {
            logger.log(Level.WARNING,"Exception",ex);
            request.setAttribute("errorName", "Exception");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("invoiceHistory");
    }

    public ActionForward allUpcomingTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                            HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            if(request.getParameter("pageNumber") == null || session.getAttribute("upcomingTicketPager") == null) {
                List<HashMap<String, String>> upcomingList = getUpcomingTicket(context.getCustomer(), null, false, true, true);
                session.setAttribute("upcomingTicketPager", Pager.create(PAGE_SIZE, upcomingList));
            }

            Pager upcomingTicketPager = (Pager) session.getAttribute("upcomingTicketPager");
            if(StringUtils.isNotBlank(request.getParameter("pageNumber"))) {
                upcomingTicketPager.setPageNumber(Integer.parseInt(request.getParameter("pageNumber")));
            }

            request.setAttribute(TICKET_INFO_LIST, upcomingTicketPager.getSubItems());

        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("upcomingTickets");
    }

    public ActionForward pastTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                     HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        if(request.getParameter("pageNumber") == null || session.getAttribute("pastTicketPager") == null) {
            List<HashMap<String, Object>> pastList = CustomerService.getPastTicketInfo(context.getCustomer().getUserID());
            session.setAttribute("pastTicketPager", Pager.create(PAGE_SIZE, pastList));
        }

        Pager pastTicketPager = (Pager) session.getAttribute("pastTicketPager");
        if(StringUtils.isNotBlank(request.getParameter("pageNumber"))) {
            pastTicketPager.setPageNumber(Integer.parseInt(request.getParameter("pageNumber")));
        }

        request.setAttribute(TICKET_INFO_LIST, pastTicketPager.getSubItems());

        return mapping.findForward("pastTickets");
    }

    public ActionForward selectTransferOrder(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> orderIds = CustomerService.getUpcomingAndTransferableOrderIds(customer.getUserID());
            if (orderIds.size() > 1) {
                Pager transferOrderPager = Pager.create(PAGE_SIZE, orderIds);
                if(StringUtils.isNotBlank(request.getParameter("pageNumber"))) {
                    transferOrderPager.setPageNumber(Integer.parseInt(request.getParameter("pageNumber")));
                }
                request.setAttribute("transferOrderPager", transferOrderPager);
                request.setAttribute(ORDER_HISTORY_LIST, getSpecifiedOrderHistory(transferOrderPager.getSubItems(), customer, -1, false));
            } else if(orderIds.size() == 1) {
                return redirect("accountManager.do?method=selectTransferTickets&order_id=" + orderIds.get(0));
            }
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("selectTransferOrder");
    }

    public ActionForward selectDonateOrder(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                           HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> orderIds = CustomerService.getUpcomingAndTransferableOrderIds(customer.getUserID());
            if (orderIds.size() > 1) {
                Pager donateOrderPager = Pager.create(PAGE_SIZE, orderIds);
                if(StringUtils.isNotBlank(request.getParameter("pageNumber"))) {
                    donateOrderPager.setPageNumber(Integer.parseInt(request.getParameter("pageNumber")));
                }
                request.setAttribute("donateOrderPager", donateOrderPager);
                request.setAttribute(ORDER_HISTORY_LIST, getSpecifiedOrderHistory(donateOrderPager.getSubItems(), customer, -1, false));
            } else if(orderIds.size() == 1) {
                return redirect("accountManager.do?method=selectDonateTickets&order_id=" + orderIds.get(0));
            }
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("selectDonateOrder");
    }

    private LinkedHashSet<String> getOrderIdsSet(List<HashMap<String, String>> orderList) {
        LinkedHashSet<String> orderIdsSet = new LinkedHashSet<>();
        for(Map<String, String> orderMap : orderList) {
            orderIdsSet.add(orderMap.get("orderId"));
        }

        return orderIdsSet;
    }

    private ActionForward selectOrder(ActionMapping mapping, HttpServletRequest request, String methodName, String forwardPage, boolean allowedReseve) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        try {

            List<Long> orderIds = CustomerService.getOrders(customer.getUserID());
            if (orderIds.size() > 1) {
                List<HashMap<String, String>> orderList = getOrderHistory(customer, HISTORY_UPCOMING, -1, allowedReseve);
                request.setAttribute(ORDER_HISTORY_LIST, orderList);
            } else {
                if (orderIds.size() == 1) {
                    return redirect("accountManager.do?method=" + methodName + "&order_id=" + orderIds.get(0));
                }

            }

        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward(forwardPage);
    }

    private void setPrintAtHomeAttribute(HttpServletRequest request, String orderId) {
        if (printAtHome(orderId)) {
            request.setAttribute("PRINT_AT_HOME", true);
        }
    }

    private boolean printAtHome(String orderId) {
        boolean printAtHome = false;
        try {
            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(orderId);
            long deliveryMethodTypeId = ticketOrder.getDeliveryMethod().getDeliveryMethodTypeId();
            printAtHome = DeliveryMethodTypeService.isPrintAtHome(deliveryMethodTypeId);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "SQLException", e);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
        }
        return printAtHome;
    }

    public ActionForward selectTransferTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               HttpServletResponse response) {

        setPrintAtHomeAttribute(request, request.getParameter("order_id"));
        request.setAttribute(SHOW_ALL_TICKETS, "FALSE");
        return selectTickets(mapping, request, "selectTransferTickets");
    }

    public ActionForward selectDonateTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) {

        request.setAttribute(SHOW_ALL_TICKETS, "TRUE");
        return selectTickets(mapping, request, "selectDonateTickets");
    }
    
    /*public ActionForward searchOrder(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) {
    	
    	return selectTickets(mapping, request, "showOrderTickets");
    }*/

    public ActionForward orderDetailShow(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                         HttpServletResponse response) {

        request.setAttribute(SHOW_ALL_TICKETS, "TRUE");
        return selectTickets(mapping, request, ORDER_DETAIL_SHOW);
    }

    public ActionForward selectTickets(ActionMapping mapping, HttpServletRequest request, String forwardPage) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        try {
            long orderId = Long.parseLong(request.getParameter("order_id"));
            List<Long> findOrderIds = new ArrayList<Long>();
            findOrderIds.add(orderId);

            boolean getAll = "TRUE".equals(request.getAttribute(SHOW_ALL_TICKETS));
            List<HashMap<String, String>> upcomingList = null;
            Customer customer = context.getCustomer();
            if (forwardPage.equals(ORDER_DETAIL_SHOW)) {
                upcomingList = getUpcomingTicket(customer, findOrderIds, getAll, false, false);
                Collections.sort(upcomingList, new TicketComparator("packageTicketId", SORT_ASC));
                TicketOrder order = TicketOrder.findByPrimaryKey(orderId);
                request.setAttribute("orderId", orderId);
                request.setAttribute("PackageTicketTotalPrice", sumPriceByPackageTicketId(upcomingList));
                request.setAttribute("orderMiscellaneousInfoTitle", MiscellaneousInfoService.MISCELLANEOUS_INFO_TITLE);
                request.setAttribute("orderMiscellaneousInfo", MiscellaneousInfoService.getInstance().buildDisplayableOrderMiscellaneousInfo(order, context.getBundle().getLocale()));
            } else {
                upcomingList = getUpcomingTicket(customer, findOrderIds, getAll, true, true);
            }
            request.setAttribute(TICKET_INFO_LIST, upcomingList);
            request.setAttribute("allowVoidReservedTickets", isAllowVoidReservedTickets(context.getOrganization().getOrganizationInfo(), orderId));
            if(forwardPage.equals("selectTransferTickets")) {
                //parse printable tickets
                parsePrintableTickets(request, upcomingList);
            }

        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING,"NumberFormatException",ex);
            request.setAttribute("errorName", ex.getMessage());
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward(forwardPage);
    }


    private final static String checkOrderIfExistedPaymentRecord=" SELECT count(*) FROM TRANSACTION WHERE ORDER_ID = ? AND (ACTION in('"+TransactionLog.ACTION_SELL+"','" + TransactionLog.ACTION_PAYMENT + "') or (ACTION = '"+TransactionLog.ACTION_RESERVE+"' and (price + tax) > 0.0001))";
    /**
     *  If there are any payments applied to the order, then the customer should not be able to return that order in Account Manager.
     * @param orgInfo
     * @param orderId
     * @return
     * @throws SQLException
     */
    private boolean isAllowVoidReservedTickets(OrganizationInfoEmbeddable orgInfo, long orderId) throws SQLException {
        if(Database.getObject(checkOrderIfExistedPaymentRecord,rs->rs.getInt(1),orderId)> 0){
            return false;
        }
        return orgInfo.isAllowVoidReservedTickets();
    }

    private Map<String, Double> sumPriceByPackageTicketId(List<HashMap<String, String>> upcomingList) {
        Map<String, Double> packageTicketPrice = new HashMap<String, Double>();
        String pkgTixIdKey = "-1";
        double sum = 0d;
        for (HashMap<String, String> ticketMap : upcomingList) {
            sum = 0d;
            pkgTixIdKey = ticketMap.get("packageTicketId");
            if (packageTicketPrice.get(pkgTixIdKey) != null) {
                sum = packageTicketPrice.get(pkgTixIdKey);
            }
            packageTicketPrice.put(pkgTixIdKey, sum + Double.parseDouble(ticketMap.get("ticketPrice")));
        }
        return packageTicketPrice;
    }

    public ActionForward orderHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                      HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        try {
            List<Long> orderIds = CustomerService.getCustomerHistoryOrderIds(customer.getUserID());
            Pager allOrderPager = Pager.create(PAGE_SIZE, orderIds);
            if(StringUtils.isNotBlank(request.getParameter("pageNumber"))) {
                allOrderPager.setPageNumber(Integer.parseInt(request.getParameter("pageNumber")));
            }
            request.setAttribute("allOrderPager", allOrderPager);
            request.setAttribute(ORDER_HISTORY_LIST, getSpecifiedOrderHistory(allOrderPager.getSubItems(), customer, -1, true));
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }

        return mapping.findForward("orderHistory");
    }

    public ActionForward showVenueInfo(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                       HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);

        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        long organizationId = customer.getOrganizationId();
		/*if (request.getParameter("org_id") != null) {
			organizationId = Long.parseLong(request.getParameter("org_id"));
		}*/
        List<String[]> venueVenueIdAndNames = OrganizationService.getInstance().getAllVenueIdAndNames(organizationId);
        if (venueVenueIdAndNames.size() < 13) {
            request.setAttribute("venueVenueIdAndNames", venueVenueIdAndNames);
            return mapping.findForward("exchangeTicket");
        } else {
            request.setAttribute("venueVenueIdAndNames", venueVenueIdAndNames);
            return mapping.findForward("showVenueList");
        }
    }

    public ActionForward pastDonate(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws SQLException, EntityNotFoundException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);

        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        List<Donation> donateList = Donation.getAllDonations(customer.getCustomerID());

        request.setAttribute("DONATE_LIST", donateList);

        return mapping.findForward(PAST_DONATES);
    }

    public ActionForward donateTicket(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws SQLException, EntityNotFoundException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);

        Customer customer = context.getCustomer();
        if (customer == null)
            return redirect(context.getLoginPage());

        long orderId = Long.parseLong(request.getParameter("order_id"));
        TicketOrder order = null;
        try {
            List<Long> cstOdrIds = CustomerService.getOrders(customer.getUserID());
            boolean hasPermission = false;
            for (long oid : cstOdrIds) {
                if (oid == orderId) {
                    hasPermission = true;
                    break;
                }
            }
            if (!hasPermission) {
                throw new AccessControlException("");
            }
            order = TicketOrder.findByPrimaryKey(orderId);
        } catch (AccessControlException e) {
            logger.log(Level.WARNING, "AccessControlException", e);
            return sendToErrorPage(mapping, request, KEY_ACCESS_DENIED, null);
        } catch (EntityNotFoundException e) {
            logger.log(Level.WARNING, "EntityNotFoundException", e);
            String[] parameters = new String[1];
            parameters[0] = String.valueOf(orderId);
            return sendToErrorPage(mapping, request, "error.orderNotFound", parameters);
        }

        String[] refundTickets = request.getParameterValues("donate_tickets");
        if (refundTickets == null) {
            return sendToErrorPage(mapping, request, "refund.selectOneTicketToRefundOrVoid", null);
        }
        try{
            request.setAttribute("donation", DonationService.getInstance().doDonate(order, customer, null, refundTickets,
                    com.etix.util.Network.getInetAddress(null).getHostAddress(),
                    Network.getRemoteAddr(request),
                    FunctionUtils.asConsumer(donate -> new TransferProcess().donateTicketEmailProcess(refundTickets, donate, customer, context.getBundle())
                    )));
        } catch (OverCapacityException e) {
            logger.log(Level.WARNING, "OverCapacityException", e);
            return sendToErrorPage(mapping, request, "OverCapacityException", null);
        } catch (RefundAmountException e) {
            logger.log(Level.WARNING, "RefundAmountException", e);
            String[] parameters = new String[1];
            parameters[0] = String.valueOf(e.getAmount());
            return sendToErrorPage(mapping, request, "itemPriceRefundableAmountExceeded", parameters);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception", e);
            return sendToErrorPage(mapping, request, "Exception", null);
        }
        return mapping.findForward(DONATE_RESULT);
    }

    private ActionForward sendToErrorPage(ActionMapping mapping, HttpServletRequest request, String errorName, String[] parameters) {
        ActionMessage actionMessage = null;
        if (parameters == null || parameters.length == 0) {
            actionMessage = new ActionMessage(errorName);
        } else {
            actionMessage = new ActionMessage(errorName, parameters);
        }

        ActionMessages actionMessages = new ActionMessages();
        actionMessages.add(errorName, actionMessage);

        saveErrors(request, actionMessages);

        return mapping.findForward(DONATE_RESULT);
    }

    /**
     * gets the upcoming tickets belong to current customer
     *
     * @param customer CustomerSignInContext
     * @return List<HashMap<String, String>>
     */
    private List<HashMap<String, String>> getUpcomingTicket(Customer customer, List<Long> findOrderIds, boolean showAllTickets, boolean filterNotDisplay, boolean upcomingTickets) throws AccessControlException, EntityNotFoundException, SQLException {

        List<Long> orderIds = new ArrayList<Long>();
        if (findOrderIds != null) {
            orderIds = findOrderIds;
        } else {
            orderIds = CustomerService.getOrders(customer.getUserID());
        }

        List<HashMap<String, String>> upcomingList = new ArrayList<HashMap<String, String>>();
        List<HashMap<String, String>> tiketsList = new ArrayList<HashMap<String, String>>();
        String action = TransferAuthorization.AUTHORIZE_ORDER;

        for (long order : orderIds) {
            // security checking
            TransferAuthorization.authorize("" + order, null, action, customer.getCustomerID());

            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(order);

            ArrayList<Ticket> transferableTickets = ticketOrder.getTransferableTickets();
            addItemToUpcomingList(ticketOrder.getCreationDate(), order, "transferable", transferableTickets, tiketsList, filterNotDisplay);

            if (showAllTickets) {
                ArrayList<Ticket> nonTransferableTickets = ticketOrder.getNonTransferableTickets();
                addItemToUpcomingList(ticketOrder.getCreationDate(), order, "nonTransferable", nonTransferableTickets, tiketsList, filterNotDisplay);

                ArrayList<Ticket> nonIssuedTickets = ticketOrder.getNonIssuedTickets();//for invoice use?
                addItemToUpcomingList(ticketOrder.getCreationDate(), order, "nonIssued", nonIssuedTickets, tiketsList, filterNotDisplay);
            }
        }

        if (upcomingTickets) {
            //remove past tickets
            Calendar currentCalendar = Calendar.getInstance();
            currentCalendar.add(Calendar.HOUR, -8);
            long currentTimeMillseconds = currentCalendar.getTimeInMillis();
            for (HashMap<String, String> map : tiketsList) {
                long perfTimeMillseconds = Long.parseLong(map.get("perfTimeMillseconds"));
                if (currentTimeMillseconds < perfTimeMillseconds) {
                    upcomingList.add(map);
                }
            }
        } else {
            upcomingList = tiketsList;
        }

        return upcomingList.stream().filter(AccountManager::isPackageAnnounced).
                sorted(new TicketComparator("perfTimeMillseconds", SORT_ASC)).collect(Collectors.toList());
    }

    /**
     * DEV-3580: Display Must Follow Package Announce Date/Time
     *
     * @param map
     * @return
     */
    private static boolean isPackageAnnounced(HashMap<String, String> map) {
        String pkgAnnouncementDateTimeInMillis = map.get("pkgAnnouncementDateTimeInMillis");
        if (pkgAnnouncementDateTimeInMillis == null) {
            return true;
        }
        boolean ret=(Calendar.getInstance().getTimeInMillis() - Long.parseLong(pkgAnnouncementDateTimeInMillis)) > 0;
        if(Environment.isTestEnvironment){
            String orderId=map.get("orderId");
            String pkgId=map.get("pkgId");
            String perfId=map.get("performanceId");
            logger.info("[AccountManager::isPackageAnnounced] {orderId:"+orderId+", pkgId:"+pkgId+", performanceId:"+perfId+"}: "+ret);
        }
        return ret ;
    }

    private List<HashMap<String, String>> getOrderHistory(Customer customer, String upcomingOrAll, int orderCount) throws AccessControlException, EntityNotFoundException, SQLException {
        return getOrderHistory(customer, upcomingOrAll, orderCount, false);
    }

    /**
     * gets the order history information, give all of the order if the parameter orderCount equals -1
     *
     * @param upcomingOrAll,  String, a flag to indicate want to get all order or just upcoming order
     * @param orderCount,     Integer, a flag to indicate the wanted orders amount
     * @param allowedReserve, Boolean, a flag to indicate whether allow shown the reserved order
     * @return List<HashMap<String, String>>
     */
    private List<HashMap<String, String>> getOrderHistory(Customer customer, String upcomingOrAll, int orderCount, boolean allowedReserve) throws AccessControlException, EntityNotFoundException, SQLException {
        List<Long> orderIds = null;
        if (HISTORY_ALL.equals(upcomingOrAll)) {
            orderIds = CustomerService.getAllOrderIds(customer.getUserID(),true);
        } else if (HISTORY_UPCOMING.equals(upcomingOrAll)) {
            orderIds = CustomerService.getOrders(customer.getUserID(),true);
        }
        orderIds.sort(Comparator.comparing(Long::longValue).reversed()); //desc order Id

        return getSpecifiedOrderHistory(orderIds, customer, orderCount, allowedReserve);
    }

    private List<HashMap<String, String>> getSpecifiedOrderHistory(List<Long> orderIds, Customer customer, int orderCount, boolean allowedReserve) throws AccessControlException, EntityNotFoundException, SQLException {
        List<HashMap<String, String>> historyList = new ArrayList<HashMap<String, String>>();
        String action = TransferAuthorization.AUTHORIZE_ORDER;

        int loopCount = 0;
        for (Long orderId : orderIds) {
            if (loopCount == orderCount) {
                break;
            }

            // security checking
            TransferAuthorization.authorize("" + orderId, null, action, customer.getCustomerID());

            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(orderId);
            SellableItem sellableItems[] = ticketOrder.getSellableItems();
            int sellableLen = sellableItems.length;

            double balance = 0.0;
            if (!(ticketOrder.getOrderOutstandingBalance() > -0.001 && ticketOrder.getOrderOutstandingBalance() < 0.001)) {
                balance = ticketOrder.getOrderOutstandingBalance();
            }

            String orderCreateTime = timeformat.format(ticketOrder.getCreationDate().getTime());
            String allTicketsVoid = "ALL_VOID";

            List<Ticket> transferableTickets = ticketOrder.getTransferableTickets();
            boolean supplement = false;
            if (allowedReserve) {
                //if allowed show reserve order check whether exist ticket with status 'reserved' and not in transfer process, if true show this order
                List<Ticket> nonTransferableTickets = ticketOrder.getNonTransferableTickets();
                List<Ticket> nonIssuedTickets = ticketOrder.getNonIssuedTickets();
                for (Ticket tix : nonIssuedTickets) {
                    if (tix.getStatus().equalsIgnoreCase(SellableItem.STATUS_RESERVED)) {
                        if (!nonTransferableTickets.contains(tix)) {
                            supplement = true;
                            break;
                        }
                    }
                }
            }

            if (transferableTickets.size() > 0 || supplement) {
                allTicketsVoid = "";
            }

            List<Long> notDisplayList = new ArrayList<Long>();
            if (sellableLen > 0) {
                List<Ticket> ticktList = new ArrayList<Ticket>(sellableItems.length);
                for (SellableItem item : sellableItems) {
                    ticktList.add((Ticket) item);
                }
                notDisplayList = TicketOrder.getNotDisplayPerformanceIds(ticktList);
            }
            Ticket ticket = null;
            int displayAmount = sellableLen;
            for (int j = 0; j < sellableLen; j++) {
                ticket = (Ticket) sellableItems[j];
                if (notDisplayList.contains(ticket.getPerformanceID()) && ticket.getPackageID() > 0) {
                    if(Environment.isTestEnvironment){
                        logger.info("[AccountManager::getOrderHistory.1] Order ("+ ticket.getOrder()+") is not allowed to show online so to be removed from displaying order list");
                    }
                    --displayAmount;
                }
            }

            for (int j = 0; j < sellableLen; j++) {
                ticket = (Ticket) sellableItems[j];
                long perfId = ticket.getPerformanceID();

                //if this performance specified not display online, skip it
                if (notDisplayList.contains(perfId) && ticket.getPackageID() > 0) {
                    if(Environment.isTestEnvironment){
                        logger.info("[AccountManager::getOrderHistory.2] Order ("+ ticket.getOrder()+") is not allowed to show online so to be removed from displaying order list");
                    }
                    continue;
                }

                Performance perf = Performance.findByPrimaryKey(perfId);
                String tmpPkgName = "";
                PackageInformation pkgInfo = null;
                if (ticket.getPackageID() > 0) {
                    tmpPkgName = com.etix.ticketing.Package.findByPrimaryKey(ticket.getPackageID()).getName();
                    //DEV-3580
                    pkgInfo = PackageInformationService.findByPrimaryKey(ticket.getPackageID());
                }

                HashMap<String, String> map = new HashMap<String, String>();
                map.put("orderId", String.valueOf(orderId));
                map.put("orderStatus", ticketOrder.getStatus());
                map.put("orderCreateTime", orderCreateTime);
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                format.setTimeZone(perf.getVenue().getTz());
                map.put("performanceTime", format.format(perf.getDateTimeWithTimeZone().getTime()));
                map.put("performanceName", perf.getName());
                //for debug use
                map.put("performanceId",perf.getPerformanceID()+"");

                List<Subscription> subscriptions = ticketOrder.getSubscription();
                if (subscriptions == null || subscriptions.isEmpty()) {
                    map.put("orderSubscriptionId", "0");
                } else {
                    map.put("orderSubscriptionId", "1");//give a fix number, since the view page will not show the number
                }
                map.put("ticketAmount", String.valueOf(displayAmount));
                map.put("orderPriceAmount", String.valueOf(ticketOrder.getTotalPriceAndFees()));
                map.put("ticketPrice", String.valueOf(ticket.getPrice() + ticket.getFeePaid()));
                map.put("isoCode", ticket.getIsoCurrency().getIsoCode());
                map.put("balance", String.valueOf(balance));
                map.put("allTicketsVoid", allTicketsVoid);
                map.put("packageName", tmpPkgName);
                if (pkgInfo != null) {
                    //DEV-3580 Display Must Follow Package Announce Date/Time
                    map.put("pkgAnnouncementDateTimeInMillis", Long.toString(pkgInfo.getAnnouncementDateTime().getTimeInMillis()));
                    map.put("pkgId",ticket.getPackageID()+"");
                }
                map.put("packageTicketId", String.valueOf(ticket.getPackageTicketID()));

                Calendar currentCalendar = Calendar.getInstance(perf.getTimeZoneObject());
                currentCalendar.add(Calendar.HOUR, -8);
                if (perf.getDateTimeWithTimeZone().compareTo(currentCalendar) < 0) {
                    map.put("pastTicket", "pastTicket");
                }

                historyList.add(map);
            }
            ++loopCount;
        }
        return historyList;
    }

    /**
     * add upcoming order information to the list
     */
    private List<HashMap<String, String>> addItemToUpcomingList(Calendar orderDate, long orderId, String status, ArrayList<Ticket> ticketList, List<HashMap<String, String>> currentList, boolean filterNotDisplay) throws SQLException, EntityNotFoundException {
        String orderCreateTime = "";
        if (orderDate != null) {
            orderCreateTime = timeformat.format(orderDate.getTime());
        }

        List<Long> notDisplayList = new ArrayList<>();
        if (ticketList.size() > 0) {
            notDisplayList = TicketOrder.getNotDisplayPerformanceIds(ticketList);
        }
        for (int i = 0; i < ticketList.size(); i++) {
            boolean displayOnline = true;
            Ticket ticket = ticketList.get(i);
            long ticketId = ticket.getTicketID();
            long perfId = ticket.getPerformanceID();

            String packageName = "";
            PackageInformation pkgInfo = null;
            if (ticket.getPackageID() > 0) {
                packageName = com.etix.ticketing.Package.findByPrimaryKey(ticket.getPackageID()).getName();
                // DEV-3580, do not use com.etix.ticketing.Package.getPackageInformation()
                pkgInfo = PackageInformationService.findByPrimaryKey(ticket.getPackageID());
            }
            if (notDisplayList.contains(perfId) && ticket.getPackageID() > 0) {
                displayOnline = false;
            }

            //if this performance specified not display online, skip it
            //if is called by show order detail(filterNotDisplay=false), controlled by attribute displayOnline
            if (filterNotDisplay && !displayOnline) {
                continue;
            }

            Performance perf = Performance.findByPrimaryKey(perfId);

            HashMap<String, String> map = new HashMap<String, String>();
            map.put("orderId", String.valueOf(orderId));
            map.put("orderStatus", status);
            map.put("orderCreateTime", orderCreateTime);
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            format.setTimeZone(perf.getVenue().getTz());
            map.put("performanceTime", format.format(perf.getDateTimeWithTimeZone().getTime()));
            map.put("perfTimeMillseconds", String.valueOf(perf.getDateTimeWithTimeZone().getTimeInMillis()));
            map.put("performanceName", perf.getName());
            map.put("performanceId",perf.getPerformanceID()+"");
            map.put("venueName", perf.getVenue().getName());
            map.put("seat", ticket.getFirstSectionName() + "&bull;" +
                    ticket.getFirstRowName() + "&bull;" +
                    ticket.getFirstSeatName());
            map.put("ticketId", String.valueOf(ticketId));
            map.put("ticketPrice", String.valueOf(ticket.getPrice() + ticket.getFeePaid()));
            map.put("isoCode", ticket.getIsoCurrency().getIsoCode());
            map.put("priceCodeName", ticket.getPriceCodeName());
            map.put("ticketStatus", ticket.getStatus());

            //add below attribute for order details page use
            map.put("displayOnline", String.valueOf(displayOnline).toUpperCase());
            map.put("packageTicketId", String.valueOf(ticket.getPackageTicketID()));
            map.put("packageName", packageName);
            map.put("packageId",String.valueOf(ticket.getPackageID()));
            if (pkgInfo != null && pkgInfo.getAnnouncementDateTime() != null) { // DEV-3580
                map.put("pkgAnnouncementDateTimeInMillis", Long.toString(pkgInfo.getAnnouncementDateTime().getTimeInMillis()));
                map.put("pkgId",ticket.getPackageID()+"");
            }
            map.put("ticketGroupId",getTicketGroupId(ticket));
            currentList.add(map);
        }
        return currentList;
    }

    private String getTicketGroupId(Ticket ticket) throws SQLException {
        if(ticket.getPackageID()>0){
            Package pack = ticket.getPackage();
            if(pack.getType() == 1){
                //flex package
                return String.valueOf(ticket.getPackageID());
            }else{
                //full package
                return String.valueOf(ticket.getPackageBarcode().getId());
            }
        }else{
            return "-1";
        }
    }

    /**
     * check if the current shipping contact is changed by customer
     *
     * @param customerID
     * @param orderID
     * @return {@code true} if changed by customer, otherwise {@code false}
     */
    public static boolean isOrderShippingContactChangedByCustomer(long customerID, long orderID) {
        CustomerProfileLog latestLog = getLatestCustomerProfileLog(customerID, orderID);
        if (latestLog == null) {
            return false;
        }

        Object shippingContactIDObj = JPAUtil.selectSingleResultBySQLQuery("select shipping_contact_id from etix_order where order_id = ?", orderID);
        Contact shippingContact = ContactService.findByPrimaryKey(shippingContactIDObj.toString());

        if (shippingContact.getDesc().equals(latestLog.getNewProfile())) {
            return true;
        }
        return false;
    }

    /**
     * get the latest customer profile log for specified order
     *
     * @param customerID
     * @param orderID
     * @return
     */
    public static CustomerProfileLog getLatestCustomerProfileLog(long customerID, long orderID) {
        String query = "select a from CustomerProfileLog a where a.customer.userID = ?1 and a.updateOrder = ?2 order by a.dateTime desc";
        List<CustomerProfileLog> logs = JPAUtil.selectListByJPQL(CustomerProfileLog.class, query, customerID, true);
        if (logs.isEmpty()) {
            return null;
        }
        CustomerProfileLog latestLog = null;
        for (int i = 0; i < logs.size(); i++) {
            CustomerProfileLog log = logs.get(i);
            // as we have sorted the log list by date time desc, so we only need to test if the order ID is contained
            if (Arrays.asList(log.getOrderIds().split(",")).contains(String.valueOf(orderID))) {
                latestLog = log;
                break;
            }
        }
        return latestLog;
    }

    public ActionForward exchangeSelectOrder(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) {

        return selectOrder(mapping, request, "exchangeSelectTickets", "exchangeSelectOrder", true);

    }

    public ActionForward exchangeSelectTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               HttpServletResponse response) {

        request.setAttribute(SHOW_ALL_TICKETS, "TRUE");
        return selectTickets(mapping, request, "exchangeSelectTickets");

    }

    private static List<Ticket> authorizeOrderSelectedExchangeTickets(String orderId, String[] selectedTickets, long customerId) throws EntityNotFoundException, SQLException, AccessControlException {
        // check this order belong to this customer
        TicketOrder order = TicketOrder.findByPrimaryKey(orderId);
        if (customerId != order.getCustomerId()) {
            logger.info("This order do not belong to this customer");
            throw new AccessControlException("This order do not belong to this customer");
        }
        List<Ticket> selectedTicketList = new ArrayList<>();
        // check each ticket belong to this order and ticket can be exchangable
        try {
            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(orderId);
            List<Ticket> transferableTickets = ticketOrder.getTransferableTickets();
            List<Ticket> nonTransferableTickets = ticketOrder.getNonTransferableTickets();
            for (int i = 0; i < selectedTickets.length; i++) {
                Ticket ticket = Ticket.findByPrimaryKey(selectedTickets[i]);
                TicketOrder currentTicketOrder = (TicketOrder) ticket.getOrder();

                if (!orderId.equals(Long.toString(currentTicketOrder.getOrderID()))) {
                    logger.info("This ticket do not belong to this customer");
                    throw new AccessControlException("This ticket do not belong to this customer");
                }
                if (!transferableTickets.contains(ticket) && !(ticket.getStatus().equalsIgnoreCase(SellableItem.STATUS_RESERVED) && !nonTransferableTickets.contains(ticket))) {
                    logger.info("ticket(s) can not be made a exchange request");
                    throw new AccessControlException("ticket(s) can not be made a exchange request");
                }
                selectedTicketList.add(ticket);
            }
        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING, "EntityNotFoundException: ", ex);
            throw ex;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "SQLException: ", ex);
            throw ex;
        }
        return selectedTicketList;
    }

    public ActionForward exchangeRequestForm(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                             HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String orderId = request.getParameter("order_id");
        String[] selectedTickets = request.getParameterValues("ticket_id");
        if (selectedTickets == null) {
            selectedTickets = new String[0];
        }

        try {
            // security checking
            authorizeOrderSelectedExchangeTickets(orderId, selectedTickets, context.getCustomer().getCustomerID());

            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(Long.parseLong(orderId));
            ArrayList<Ticket> selectTransferTickets = new ArrayList<Ticket>();

            for (int i = 0; i < selectedTickets.length; i++) {
                selectTransferTickets.add(Ticket.findByPrimaryKey(selectedTickets[i]));
            }

            List<HashMap<String, String>> ticketsList = new ArrayList<HashMap<String, String>>();
            addItemToUpcomingList(ticketOrder.getCreationDate(), Long.parseLong(orderId), "transferable", selectTransferTickets, ticketsList, true);

            if (ticketsList.size() == 0) {
                ticketsList = null;
            }

            request.setAttribute(TICKET_INFO_LIST, ticketsList);
            return mapping.findForward("exchangeRequestForm");
        } catch (EntityNotFoundException ex) {
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    /**
     * After the customer submit the ticket exchange request form, an email with the request detail informations
     * will be sent to the organization "ticket exchange email" address(as reply to email also), and use system mail address
     * as the from, at the same time use customer email address as CC(make the customer can contact the organization directly)
     */
    public ActionForward exchangeRequestResult(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        Customer customer = context.getCustomer();
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());

        String orderId = request.getParameter("order_id");
        String[] selectedTickets = request.getParameterValues("ticket_id");
        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String customerEmailAsCC = request.getParameter("email");
        String dayAreaCode = request.getParameter("day_area_code");
        String dayPhone = request.getParameter("day_phone");
        String exchangeTo = request.getParameter("exchange_to");
        String exchangeSpecialInfo = request.getParameter("exchange_special_info");

        if (orderId == null || selectedTickets == null || selectedTickets.length == 0 || "".equals(firstName.trim()) || "".equals(lastName.trim()) || customerEmailAsCC.indexOf("@") <= 0 || customerEmailAsCC.indexOf(".") <= 0 || "".equals(dayPhone.trim()) || "".equals(exchangeTo)) {
            request.setAttribute("errorName", "errors.encodeUtils.inputText.isnull");
            return mapping.findForward(ERROR);
        }

        try {
            // security checking
            authorizeOrderSelectedExchangeTickets(orderId, selectedTickets, customer.getCustomerID());

            ResourceBundle bundle = context.getBundle();

            Organization organization = customer.getOrganization();
            OrganizationInfoEmbeddable orgInfo = OrganizationService.getInstance().findByPrimaryKeyJPA(customer.getOrganizationId()).getOrganizationInfo();
            String orgName = "";
            String emailFrom = "thankyou@etix.com";
            String emailFromAlias = bundle.getString("accountManager.exchange.fromAlias");
            String emailTo = "";
            if (orgInfo != null) {
                orgName = orgInfo.getPublicName();
                //get the organization used email, exchange email is prior, then public email, contact email finally
                emailTo = orgInfo.getTicketExchangeEmail();
                if (emailTo == null || "".equals(emailTo.trim())) {
                    emailTo = orgInfo.getPublicEmail();
                }
                if (emailTo == null || "".equals(emailTo.trim())) {
                    emailTo = organization.getContact().getEmail();
                }
            }
            if ("".equals(orgName)) {
                orgName = organization.getName();
            }

            Locale locale = bundle.getLocale();
            locale = InternationalizationUtil.getLocaleWithCountryCode(locale);
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, locale);

            StringBuilder msg = new StringBuilder();
            msg.append(orgName + ",\n\n");

            String subject = bundle.getString("accountManager.exchangeEmail.subject");
            StringBuilder customerInfo = new StringBuilder();
            customerInfo.append(bundle.getString("accountManager.exchangeEmail.customerProfile"));
            customerInfo.append(bundle.getString("userName") + ": " + customer.getName() + "\n");
            customerInfo.append(bundle.getString("firstName") + ": " + firstName + "\n");
            customerInfo.append(bundle.getString("lastName") + ": " + lastName + "\n");
            customerInfo.append(bundle.getString("email") + ": " + customerEmailAsCC + "\n");
            customerInfo.append(bundle.getString("dayPhone") + ": " + dayAreaCode + " " + dayPhone + "\n\n");

            msg.append(customerInfo);

            msg.append(MessageFormat.format(bundle.getString("accountManager.exchangeEmail.exchangeTickets"), new Object[]{orderId}));

            StringBuilder ticketsContent = new StringBuilder();

            boolean existPackage = false;
            for (String ticketId : selectedTickets) {
                Ticket ticket = Ticket.findByPrimaryKey(ticketId);
                String packName = null;
                if (ticket.getPackageID() > 0) {
                    existPackage = true;
                    packName = com.etix.ticketing.Package.findByPrimaryKey(ticket.getPackageID()).getName(locale);
                }
                Performance perf = ticket.getPerformance();
                dateFormat.setTimeZone(perf.getVenue().getTz());
                String perfName = perf.getName();
                String seatInfo = ticket.getFirstSectionName() + " - " + ticket.getFirstRowName() + " - " + ticket.getFirstSeatName();
                ticketsContent.append(ticket.getStatus() + "  " + (packName != null ? (packName + "  ") : "") + perfName + "(" +
                        dateFormat.format(perf.getDateTimeWithTimeZone().getTime()) + ")  " + perf.getVenue().getName() + "  " + seatInfo + "\n");
            }

            String blank = "                    ";
            msg.append(bundle.getString("status") + blank + (existPackage ? (bundle.getString("package") + blank) : "") + bundle.getString("performance") + "/" +
                    bundle.getString("date") + blank + bundle.getString("venue") + blank + bundle.getString("accountManager.sectionRowSeat") + "\n" +
                    "------------------------------------------------------------------------------------------------------------\n");
            msg.append(ticketsContent + "\n");

            String exchangeToContent = MessageFormat.format(bundle.getString("accountManager.exchangeEmail.exchangeTo"), new Object[]{exchangeTo});
            String exchangeOtherContent = MessageFormat.format(bundle.getString("accountManager.exchangeEmail.exchangeAccommodation"), new Object[]{exchangeSpecialInfo});

            msg.append(exchangeToContent);
            msg.append(exchangeOtherContent);

            MailTransfer mail = new MailTransfer(locale);
            try {
                EmailMessage em = new EmailMessage(emailTo, customerEmailAsCC, "", subject, msg.toString());
                em.setFromAlias(emailFromAlias);
                em.setFromEmail(emailFrom);
                em.setReplyTo(emailTo);
                mail.sendMessage(em);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "email from: " + emailFrom +
                        "email to: " + emailTo +
                        "email cc: " + customerEmailAsCC +
                        "message:" + msg, ex);
                request.setAttribute("errorName", "accountManager.exchange.error");
                return mapping.findForward(ERROR);
            }

            return mapping.findForward("exchangeRequestResult");

        } catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        } catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }
    }

    public ActionForward getPrintOrdersTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws SQLException, EntityNotFoundException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null) {
            return mapping.findForward(SIGN_IN);
        }
        if (context.getCustomer() == null) {
            return redirect(context.getLoginPage());
        }
        List<Long> orderIds = CustomerService.getOrders(context.getCustomer().getUserID());
        List<String[]> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (long orderId : orderIds) {
            names.clear();
            sb.setLength(0);
            boolean isShowPrintTicket = true;
            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(orderId);
            DeliveryMethod deliveryMethod = DeliveryMethodService.findByPrimaryKey(ticketOrder.getDeliveryMethodID());
            if (!DeliveryMethodTypeService.isPrintAtHome(deliveryMethod.getDeliveryMethodTypeId())) {
                isShowPrintTicket = false;
            }

            ArrayList<Ticket> transferableTickets = ticketOrder.getTransferableTickets();
            for (Ticket ticket : transferableTickets) {
                if (ticket.getPackageID() > -1) {
                    boolean isDisplayOnline = PackagePerformanceService.getPpByPackAndPerf(ticket.getPackageID(), ticket.getPerformanceID()).isDisplayOnline();
                    if (isDisplayOnline) {
                        names.add(ticket.getPerformance().getName());
                        names.add(ticket.getPackage().getName());
                    }
                } else {
                    names.add(ticket.getPerformance().getName());
                }
            }

            if (names.size() > 0) {
                result.add(new String[]{String.valueOf(orderId), String.join("<br/>", names), ticketOrder.getConfirmationCode(), String.valueOf(isShowPrintTicket), deliveryMethod.getName()});
            }
        }
        request.setAttribute("orderPrintList", result);
        return mapping.findForward("printOrdersTickets");
    }

    /**
     * sort upcoming ticket info list
     */
    private class TicketComparator implements Comparator<HashMap<String, String>> {
        private String compareKey;
        private String sortDirection;

        public TicketComparator(String compareKey, String sortDirection) {
            this.compareKey = compareKey;
            this.sortDirection = sortDirection;
        }

        @Override
        public int compare(HashMap<String, String> arg0, HashMap<String, String> arg1) {
            int result = 0;
            try {
                HashMap<String, String> map0 = (HashMap<String, String>) arg0;
                HashMap<String, String> map1 = (HashMap<String, String>) arg1;

                result = map0.get(compareKey).compareTo(map1.get(compareKey));
                if (SORT_DESC.equals(sortDirection)) {
                    result *= -1;
                }
            } catch (Exception e) {
                // nothing todo
                logger.log(Level.WARNING,"Exception",e);
            }

            return result;
        }
    }

    private void setMessage(HttpServletRequest request, String messageName) {
        ActionMessages messsages = new ActionMessages();
        ActionMessage newMessage = new ActionMessage(messageName);
        messsages.add(ActionMessages.GLOBAL_MESSAGE, newMessage);
        saveMessages(request, messsages);
    }

    private final static String INS_CUSTOMER_VOID_TICKET="INSERT INTO CUSTOMER_VOID_TICKET(ID, TICKET_ID, CUSTOMER_ID, VOID_DATETIME, BATCH_ID) VALUES(?, ?, ?, ?, ?)";
    /**
     * DEV-6655
     * @param mapping
     * @param form
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    public ActionForward removeReservedTickets(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                               HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null)
            return mapping.findForward(SIGN_IN);
        if (context.getCustomer() == null)
            return redirect(context.getLoginPage());
        try {
            String orderId=request.getParameter("order_id");
            if(!isAllowVoidReservedTickets(context.getOrganization().getOrganizationInfo(),Long.parseLong(orderId))){
                request.setAttribute("errorName", "accountManager.notAllowToVoidTicket");
                return mapping.findForward(ERROR);
            }
            String[] ticketIds = request.getParameterValues("ticket_id");
            String[] ticketIdsFull = request.getParameterValues("ticket_id_full");
            String[] packageIdFull = request.getParameterValues("package_id_full");
            String[] ticketGroupIdFull = request.getParameterValues("ticket_group_id_full");

            TicketGroupChecker ticketGroupChecker = buildTicketGroupChecker(ticketIdsFull, packageIdFull, ticketGroupIdFull);
            ticketGroupChecker.checkPackageGroupRestriction(ticketIds);

            List<Ticket> ticketsSelected = authorizeOrderSelectedExchangeTickets(orderId, ticketIds, context.getCustomer().getCustomerID());
            TicketOrder ticketOrder = TicketOrder.findByPrimaryKey(orderId);
            CustomerVoidTicketService voidTicketService=Injector.Factory.getInjector().get(CustomerVoidTicketService.class);
            TransactionComponentDetailService transComponentDetailService = Injector.Factory.getInjector().get(TransactionComponentDetailService.class);
            boolean ret=JPAUtil.save((em, conn) -> {
                Map<Long,List<TransactionComponentDetail>> transactonComponentDetailMap = new HashMap<>();
                for(Ticket ticket:ticketsSelected){
                    List<TransactionComponentDetail> tcdList=transComponentDetailService.findTransactionComponentDetialByTicket(em,ticket.getTicketID());
                    transactonComponentDetailMap.put(ticket.getTicketID(),tcdList);
                }
                voidTicketService.voidUnpaidTickets(ticketOrder,ticketsSelected,transactonComponentDetailMap,"Void Ticket from AccountManager", Network.getInetAddress(null).getHostAddress(),Network.getRemoteAddr(request),conn);
                logCustomerVoidTicket(context.getCustomer().getCustomerID(), ticketsSelected, conn);
            },null);
            if(ret){
                voidTicketService.sendVoidTicketEmail(WebLocale.getLocale(request),context.getBundle(),context.getOrganization(),context.getCustomer(),ticketsSelected);
            }
        }catch (EntityNotFoundException ex) {
            logger.log(Level.WARNING,"EntityNotFoundException",ex);
            request.setAttribute("errorName", "EntityNotFoundException");
            return mapping.findForward(ERROR);
        } catch (SQLException ex) {
            logger.log(Level.WARNING,"SQLException",ex);
            request.setAttribute("errorName", "IOException");
            return mapping.findForward(ERROR);
        }catch (AccessControlException ex) {
            logger.log(Level.WARNING,"AccessControlException",ex);
            request.setAttribute("errorName", KEY_ACCESS_DENIED);
            return mapping.findForward(ERROR);
        }catch(IllegalArgumentException ex){
            logger.log(Level.WARNING,"IllegalArgumentException",ex);
        }
        ActionForward af = new ActionForward(mapping.findForward(ORDER_DETAIL_SHOW));
        af.setPath(new StringBuilder().append("/accountManager/accountManager.do?method=orderDetailShow&order_id=").append(request.getParameter("order_id")).toString());
        af.setRedirect(true);
        return af;
    }

    private TicketGroupChecker buildTicketGroupChecker(String[] ticketIdsFull, String[] packageIdFull, String[] ticketGroupIdFull) {
        TicketGroupChecker checker = new TicketGroupChecker();
        for (int i = 0; i < ticketIdsFull.length; i++) {
            checker.ticketPackagePair.put(ticketIdsFull[i], Long.parseLong(packageIdFull[i]));
            checker.ticketGroupPair.put(ticketIdsFull[i], ticketGroupIdFull[i]);
            Set<String> ticketsSet = checker.ticketsInSameGroupMap.getOrDefault(ticketGroupIdFull[i], new HashSet<>());
            ticketsSet.add(ticketIdsFull[i]);
            checker.ticketsInSameGroupMap.put(ticketGroupIdFull[i], ticketsSet);
        }
        return checker;
    }

    private class TicketGroupChecker {
        //key is ticketId, value is packageId
        private Map<String, Long> ticketPackagePair = new HashMap<>();
        //key is ticketId, value is groupId
        private Map<String, String> ticketGroupPair = new HashMap<>();
        //key is ticketGroupId, value is relative tickets
        private Map<String, Set<String>> ticketsInSameGroupMap = new HashMap<>();

        public void checkPackageGroupRestriction(String[] ticketIds) {
            List<String> packageTickets = new ArrayList<>();
            for (String ticketId : ticketIds) {
                if (ticketPackagePair.get(ticketId) > 0) {
                    packageTickets.add(ticketId);
                }
            }
            if (packageTickets.isEmpty()) {
                return;
            }
            Map<String, Set<String>> ticketsGroupByGroupId = new HashMap<>();
            for (String ticketId : packageTickets) {
                String ticketGroupId = ticketGroupPair.get(ticketId);
                Set<String> ticketsSet = ticketsGroupByGroupId.getOrDefault(ticketGroupId, new HashSet<>());
                ticketsSet.add(ticketId);
                ticketsGroupByGroupId.put(ticketGroupId, ticketsSet);
            }
            for (Map.Entry<String, Set<String>> me : ticketsGroupByGroupId.entrySet()) {
                if (!CollectionUtils.isEqualCollection(ticketsInSameGroupMap.get(me.getKey()), me.getValue())) {
                    throw new IllegalArgumentException("Not satisfying the package group restriction");
                }
            }
        }
    }

    public ActionForward getTicketLimitInformation(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return mapping.findForward(SIGN_IN);
        }
        CustomerSignInContext context = (CustomerSignInContext) session.getAttribute(CustomerSignInContext.CONTEXT_OBJ);
        if (context == null) {
            return mapping.findForward(SIGN_IN);
        }
        if (context.getCustomer() == null) {
            return redirect(context.getLoginPage());
        }

        String subscriptionId = request.getParameter("subscriptionId");
        Subscription subscription = SubscriptionService.getInstance().findByPrimaryKey(subscriptionId);
        StringBuilder stringBuilder = new StringBuilder();
        if (!subscription.getMembershipLevel().isCredentialType()) {
            MembershipTicketsResults membershipTicketsResults = new MembershipTicketsResults();
            membershipTicketsResults.setSubscription(subscription);
            List<SubscriptionPerformanceRecord> subscriptionPerformanceRecordList = membershipTicketsResults.findSubscriptionPerformanceRecordListByTicketLimit(true);
            List<SubscriptionPackageRecord> subscriptionPackageRecordList = membershipTicketsResults.findSubscriptionPackageRecordListByTicketLimit(true);
            List<SubscriptionEventSeriesRecord> subscriptionEventRecordList = membershipTicketsResults.findSubscriptionEventRecordListByTicketLimit(true);
            List<SubscriptionOrganizationGroupRecord> subscriptionOrganizationGroupRecordList = membershipTicketsResults.findSubscriptionOrganizationGroupRecordListByMemshpLevel(true);
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL, context.getBundle().getLocale());
            for (SubscriptionPerformanceRecord subscriptionPerformanceRecord : subscriptionPerformanceRecordList) {
                String performanceNameDate = subscriptionPerformanceRecord.getPerformance().getName() + " - " + dateFormat.format(subscriptionPerformanceRecord.getPerformance().getDateTime().getTime());
                List<PriceCode> priceCodes = subscriptionPerformanceRecord.getItems().stream().map(spr -> spr.getPriceCode()).collect(Collectors.toList());
                int purchasedCount = subscriptionPerformanceRecord.getItems().stream().mapToInt(spr -> spr.getPurchased() * spr.getTicketCountValue()).sum();
                stringBuilder.append(getTicketLimitRowInfo(subscriptionPerformanceRecord.getTicketLimit(), performanceNameDate, priceCodes, purchasedCount, context.getBundle()));
            }
            for (SubscriptionPackageRecord subscriptionPackageRecord : subscriptionPackageRecordList) {
                String packageName = subscriptionPackageRecord.getPack().getName();
                List<PriceCode> priceCodes = subscriptionPackageRecord.getItems().stream().map(spr -> spr.getPriceCode()).collect(Collectors.toList());
                int purchasedCount = subscriptionPackageRecord.getItems().stream().mapToInt(spr -> spr.getPurchased() * spr.getTicketCountValue()).sum();
                stringBuilder.append(getTicketLimitRowInfo(subscriptionPackageRecord.getTicketLimit(), packageName, priceCodes, purchasedCount, context.getBundle()));
            }
            for (SubscriptionEventSeriesRecord subscriptionEventSeriesRecord : subscriptionEventRecordList) {
                String eventName = subscriptionEventSeriesRecord.getEvent().getName();
                List<PriceCode> priceCodes = subscriptionEventSeriesRecord.getItems().stream().map(spr -> spr.getPriceCode()).collect(Collectors.toList());
                int purchasedCount = subscriptionEventSeriesRecord.getItems().stream().mapToInt(spr -> spr.getPurchased() * spr.getTicketCountValue()).sum();
                stringBuilder.append(getTicketLimitRowInfo(subscriptionEventSeriesRecord.getTicketLimit(), eventName, priceCodes, purchasedCount, context.getBundle()));
            }
            for (SubscriptionOrganizationGroupRecord subscriptionOrganizationGroupRecord : subscriptionOrganizationGroupRecordList) {
                String groupName = subscriptionOrganizationGroupRecord.getPerformanceGroup().getName();
                List<PriceCode> priceCodes = subscriptionOrganizationGroupRecord.getItems().stream().map(spr -> spr.getPriceCode()).collect(Collectors.toList());
                int purchasedCount = subscriptionOrganizationGroupRecord.getItems().stream().mapToInt(spr -> spr.getPurchased() * spr.getTicketCountValue()).sum();
                stringBuilder.append(getTicketLimitRowInfo(subscriptionOrganizationGroupRecord.getTicketLimit(), groupName, priceCodes, purchasedCount, context.getBundle()));
            }
        }
        response.getWriter().print(stringBuilder.toString());
        return null;
    }

    private String getTicketLimitRowInfo(TicketLimit ticketLimit, String eventName, List<PriceCode> priceCodes, int purchasedCount, ResourceBundle bundle) {
        StringBuilder stringRowBuilder = new StringBuilder();
        if (ticketLimit.isShowRemainingTicket()) {
            stringRowBuilder.append("<tr>");
            stringRowBuilder.append("<td>").append(eventName).append("</td>");
            stringRowBuilder.append("<td>").append(StringUtil.toPrettyCommaDelimitedString(priceCodes.stream().map(priceCode -> priceCode.getName()).collect(Collectors.toList()))).append("</td>");
            if (ticketLimit.getMaximumTicket() == -1) {
                stringRowBuilder.append("<td></td>");
                stringRowBuilder.append("<td>").append(bundle.getString("unlimited")).append("</td>");
            } else {
                int remainingCount = ticketLimit.getMaximumTicket() - purchasedCount;
                stringRowBuilder.append("<td>").append(remainingCount).append("</td>");
                stringRowBuilder.append("<td>").append(ticketLimit.getMaximumTicket()).append("</td>");
            }
            stringRowBuilder.append("</tr>");
        }
        return stringRowBuilder.toString();
    }

    private long logCustomerVoidTicket(long customerId, List<Ticket> ticketsSelected, Connection conn) throws SQLException {
        Calendar voidCal = GregorianCalendar.getInstance();
        long batchId = Database.getNextValueFromSequence("CUSTOMER_VOID_TICKET_BATCH_SEQ", conn);
        for (Ticket ticket : ticketsSelected) {
            long seqId = Database.getNextValueFromSequence("CUSTOMER_VOID_TICKET_SEQ", conn);
            Database.executeUpdate(conn, INS_CUSTOMER_VOID_TICKET, new Object[]{seqId, ticket.getTicketID(), customerId, voidCal, batchId});
        }
        return batchId;
    }

}
