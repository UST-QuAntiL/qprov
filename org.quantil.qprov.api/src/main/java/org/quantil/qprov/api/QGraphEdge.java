package org.quantil.qprov.api;

import lombok.Data;
import org.springframework.web.bind.annotation.ResponseBody;

@Data
@ResponseBody
public class QGraphEdge {
    public String id;
    public String label;
    public int source;
    public int target;
}
