import net.velocloud.swagger.client.VCApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.model.*;
import net.velocloud.swagger.api.*;

import com.sun.jersey.api.client.ClientResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.*;

public class RoutedInterfacesExample {

  private static final AllApi api = new AllApi();
  private static ObjectMapper mapper = new ObjectMapper();

  public static void main(String[] args) {

      // EDIT PARAMS AS NEEDED
      String HOSTNAME = "HOSTNAME";
      String USERNAME = "USERNAME";
      String PASSWORD = "PASSWORD";

      int edgeId = 105;
      int enterpriseId = 5;

      String interfaceToUpdate = "GE4";
      int subinterfaceToUpdate = 1234;

      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      RoutedInterfacesExample.api.setApiClient(new VCApiClient());
      api.getApiClient().setBasePath("https://"+HOSTNAME+"/portal/rest");
      AuthObject authorization = new AuthObject();
      authorization.setUsername(USERNAME);
      authorization.setPassword(PASSWORD);
      try {
        api.loginOperatorLogin(authorization);
      } catch (ApiException e) {
        System.out.println("Error in OperatorLogin. Is the VCO Portal running and are your credentials valid?");
        return;
      }

      // Pull down current deviceSettings module, given an edgeId and (since we're
      // making this call as an operator user) the enterpriseId
      EdgeDeviceSettings module = getEdgeDeviceSettingsFromStack(edgeId, enterpriseId);
      List<EdgeDeviceSettingsDataRoutedInterfaces> routedInterfaces = module.getData().getRoutedInterfaces();
      // Find/update the target routed interface
      for ( EdgeDeviceSettingsDataRoutedInterfaces routedInterface : routedInterfaces ) {
        if ( routedInterface.getName().equals(interfaceToUpdate) ) {

          // Find/update the target subinterface
          List<EdgeDeviceSettingsDataSubinterfaces> subinterfaces = new ArrayList<EdgeDeviceSettingsDataSubinterfaces>();
          EdgeDeviceSettingsDataSubinterfaces subinterface = new EdgeDeviceSettingsDataSubinterfaces();

          // Subinterface addressing
          EdgeDeviceSettingsDataAddressing1 subinterfaceAddressing = new EdgeDeviceSettingsDataAddressing1();
          subinterfaceAddressing
            .type(EdgeDeviceSettingsDataAddressing1.TypeEnum.STATIC)
            .cidrIp("12.80.1.21")
            .cidrPrefix(28)
            .gateway("12.80.1.19")
            .netmask("255.255.255.240");

          subinterface
            .subinterfaceId(subinterfaceToUpdate)
            .subinterfaceType("SUB_INTERFACE")
            .disabled(Boolean.FALSE)
            .addressing(subinterfaceAddressing)
            .advertise(Boolean.FALSE)
            .natDirect(Boolean.FALSE)
            .trusted(Boolean.TRUE)
            .rpf(EdgeDeviceSettingsDataSubinterfaces.RpfEnum.DISABLED)
            .vlanId(subinterfaceToUpdate);

          subinterfaces.add(subinterface);

          EdgeDeviceSettingsDataL2 l2 = new EdgeDeviceSettingsDataL2();
          l2.autonegotiation(Boolean.TRUE)
            .MTU(1500)
            .duplex("FULL")  // Default value, inherited from the profile interface config
            .speed("100M");  // Default value, inherited from the profile interface config

          routedInterface.getAddressing()
            .type(EdgeDeviceSettingsDataAddressing.TypeEnum.STATIC)
            .cidrIp("32.40.67.208")
            .cidrPrefix(32)
            .netmask("255.255.255.255");

          routedInterface
            .override(Boolean.TRUE)
            .disabled(Boolean.FALSE)
            .wanOverlay(EdgeDeviceSettingsDataRoutedInterfaces.WanOverlayEnum.DISABLED)
            .advertise(Boolean.TRUE)
            .natDirect(Boolean.FALSE)
            .underlayAccounting(Boolean.FALSE)
            .trusted(Boolean.TRUE)
            .rpf(EdgeDeviceSettingsDataRoutedInterfaces.RpfEnum.DISABLED)
            .l2(l2)
            .subinterfaces(subinterfaces);

          break;
        }
      }

      updateConfigurationModule(module, enterpriseId);
      System.out.format("Successfully updated interface (%s) and subinterface (%d)\n", interfaceToUpdate, subinterfaceToUpdate);

  }

  private static EdgeDeviceSettings getEdgeDeviceSettingsFromStack(int edgeId, int enterpriseId) {

    EdgeGetEdgeConfigurationStack req = new EdgeGetEdgeConfigurationStack()
                                              .enterpriseId(enterpriseId)
                                              .edgeId(edgeId);

    List<EdgeGetEdgeConfigurationStackResultItem> res = null;
    try {
      res = api.edgeGetEdgeConfigurationStack(req);
    } catch (ApiException e) {
      System.out.println("Exception in edge/getEdgeConfigurationStack: " + e);
      System.exit(-1);
    }
    // the Edge config is always the first entry
    EdgeGetEdgeConfigurationStackResultItem edgeConfig = res.get(0);

    for ( ConfigurationModule module : edgeConfig.getModules() ) {
      if ( module.getName().toString().equals("deviceSettings") )
        return mapper.convertValue(module, EdgeDeviceSettings.class);
    }
    return null;

  }

  private static void updateConfigurationModule(ConfigurationModule update, int enterpriseId) {

    ConfigurationUpdateConfigurationModule req = new ConfigurationUpdateConfigurationModule();
    req.setId(update.getId());
    req.setEnterpriseId(enterpriseId);
    req.setUpdate(update);

    ConfigurationUpdateConfigurationModuleResult res = null;
    try {
      res = api.configurationUpdateConfigurationModule(req);
    } catch (ApiException e) {
      System.out.println("Exception in configuration/updateConfigurationModule: " + e);
      System.exit(-1);
    }
    return;
  }

}
