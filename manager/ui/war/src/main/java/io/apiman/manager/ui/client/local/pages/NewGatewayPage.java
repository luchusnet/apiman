/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.manager.ui.client.local.pages;

import io.apiman.manager.api.beans.gateways.GatewayBean;
import io.apiman.manager.api.beans.gateways.GatewayType;
import io.apiman.manager.api.beans.gateways.NewGatewayBean;
import io.apiman.manager.api.beans.gateways.RestGatewayConfigBean;
import io.apiman.manager.api.beans.summary.GatewayTestResultBean;
import io.apiman.manager.ui.client.local.AppMessages;
import io.apiman.manager.ui.client.local.pages.admin.GatewayTestResultDialog;
import io.apiman.manager.ui.client.local.services.BeanMarshallingService;
import io.apiman.manager.ui.client.local.services.rest.IRestInvokerCallback;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.errai.ui.nav.client.local.Page;
import org.jboss.errai.ui.nav.client.local.PageShown;
import org.jboss.errai.ui.nav.client.local.TransitionTo;
import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.overlord.commons.gwt.client.local.widgets.AsyncActionButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;


/**
 * Page that lets the user create a new gateway.
 *
 * @author eric.wittmann@redhat.com
 */
@Templated("/io/apiman/manager/ui/client/local/site/new-gateway.html#page")
@Page(path="new-gateway")
@Dependent
public class NewGatewayPage extends AbstractPage {

    @Inject
    BeanMarshallingService marshaller;
    
    @Inject
    TransitionTo<AdminGatewaysPage> toGateways;
    
    @Inject @DataField
    TextBox name;
    @Inject @DataField
    TextArea description;

    @Inject @DataField
    TextBox configEndpoint;
    @Inject @DataField
    TextBox username;
    @Inject @DataField
    TextBox password;
    @Inject @DataField
    TextBox passwordConfirm;

    @Inject @DataField
    AsyncActionButton testButton;
    @Inject @DataField
    AsyncActionButton createButton;
    
    @Inject
    Instance<GatewayTestResultDialog> resultDialogFactory;
    
    /**
     * Constructor.
     */
    public NewGatewayPage() {
    }

    @PostConstruct
    protected void postConstruct() {
        KeyUpHandler handler = new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                enableCreateButtonIfValid();
            }
        };
        name.addKeyUpHandler(handler);
        configEndpoint.addKeyUpHandler(handler);
        username.addKeyUpHandler(handler);
        password.addKeyUpHandler(handler);
        passwordConfirm.addKeyUpHandler(handler);
    }
    
    /**
     * Enables the create button only if the contents of the form are valid.
     */
    protected void enableCreateButtonIfValid() {
        String n = name.getValue();
        String ce = configEndpoint.getValue();
        String u = username.getValue();
        String p1 = password.getValue();
        String p2 = passwordConfirm.getValue();
        boolean valid = true;
        if (n == null || n.trim().length() == 0) {
            valid = false;
        }
        if (ce == null || ce.trim().length() == 0) {
            valid = false;
        }
        if (u == null || u.trim().length() == 0) {
            valid = false;
        }
        if (p1 != null & p1.trim().length() > 0) {
            if (!p1.equals(p2)) {
                valid = false;
            }
        } else {
            valid = false;
        }
        testButton.setEnabled(valid);
        setTestButtonClass("warning"); //$NON-NLS-1$
        createButton.setEnabled(valid);
    }

    /**
     * Called once the page is shown.
     */
    @PageShown
    protected void onPageShown() {
        name.setFocus(true);
        createButton.reset();
        createButton.setEnabled(false);
        testButton.reset();
        testButton.setEnabled(false);
    }

    /**
     * Called when the user clicks the Create Gateway button.
     * @param event
     */
    @EventHandler("createButton")
    public void onCreate(ClickEvent event) {
        createButton.onActionStarted();
        NewGatewayBean gateway = getGatewayFromForm();
        rest.createGateway(gateway, new IRestInvokerCallback<GatewayBean>() {
            @Override
            public void onSuccess(GatewayBean response) {
                toGateways.go();
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }
    
    /**
     * Called when the user clicks the Test button.
     * @param event
     */
    @EventHandler("testButton")
    public void onTest(ClickEvent event) {
        testButton.onActionStarted();
        NewGatewayBean gateway = getGatewayFromForm();
        rest.testGateway(gateway, new IRestInvokerCallback<GatewayTestResultBean>() {
            @Override
            public void onSuccess(GatewayTestResultBean response) {
                testButton.onActionComplete();
                if (response.isSuccess()) {
                    setTestButtonClass("success"); //$NON-NLS-1$
                } else {
                    setTestButtonClass("danger"); //$NON-NLS-1$
                    GatewayTestResultDialog dialog = resultDialogFactory.get();
                    dialog.setResultDetails(response.getDetail());
                    dialog.show();
                }
            }
            @Override
            public void onError(Throwable error) {
                dataPacketError(error);
            }
        });
    }

    /**
     * @return a gateway bean from the info the user entered in the form
     */
    protected NewGatewayBean getGatewayFromForm() {
        NewGatewayBean gateway = new NewGatewayBean();
        gateway.setName(name.getValue().trim());
        gateway.setDescription(description.getValue().trim());
        gateway.setType(GatewayType.REST);
        RestGatewayConfigBean configBean = new RestGatewayConfigBean();
        configBean.setEndpoint(configEndpoint.getValue().trim());
        configBean.setUsername(username.getValue().trim());
        if (password.getValue() != null && password.getValue().trim().length() > 0) {
            configBean.setPassword(password.getValue().trim());
        }
        gateway.setConfiguration(marshaller.marshal(configBean));
        return gateway;
    }

    /**
     * @param status
     */
    private void setTestButtonClass(String status) {
        testButton.getElement().removeClassName("btn-success"); //$NON-NLS-1$
        testButton.getElement().removeClassName("btn-warning"); //$NON-NLS-1$
        testButton.getElement().removeClassName("btn-danger"); //$NON-NLS-1$
        testButton.getElement().addClassName("btn-" + status); //$NON-NLS-1$
    }
    
    /**
     * @see io.apiman.manager.ui.client.local.pages.AbstractPage#getPageTitle()
     */
    @Override
    protected String getPageTitle() {
        return i18n.format(AppMessages.TITLE_NEW_GATEWAY);
    }

}
