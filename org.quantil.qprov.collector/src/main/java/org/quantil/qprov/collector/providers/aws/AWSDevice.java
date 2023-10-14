package org.quantil.qprov.collector.providers.aws;

import lombok.Data;

@Data
public class AWSDevice {
    private String deviceArn;
    private String deviceName;
    private String providerName;
    private String deviceType;
    private String deviceStatus;
    private String deviceCapabilities;
}
