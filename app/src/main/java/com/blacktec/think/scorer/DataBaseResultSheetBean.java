package com.blacktec.think.scorer;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Think on 2018/11/6.
 */
public class DataBaseResultSheetBean {
    private List<BridgeResultSheetBean> tables;
    public DataBaseResultSheetBean() {}
    public DataBaseResultSheetBean(List<BridgeResultSheet> list)
    {
        tables= new ArrayList<>();
        for(BridgeResultSheet sheet: list)
        {
            List<BridgeContract> brgContracts= sheet.getContracts();
            BridgeResultSheetBean resultSheetBean = new BridgeResultSheetBean();
            resultSheetBean.setTableId(sheet.getID().toString());
            resultSheetBean.setTableInfo(sheet.getTableInfo());
            List<BridgeResultSheetBean.BridgeContractBean> contractsBean= new ArrayList<>();
            for(BridgeContract c: brgContracts)
            {
                BridgeResultSheetBean.BridgeContractBean contractBean = resultSheetBean.new BridgeContractBean();
                contractBean.setBrdNum(c.getBoardNum());
                contractBean.setContract(c.getResultText());
                contractsBean.add(contractBean);
            }
            resultSheetBean.setContracts (contractsBean);
            tables.add(resultSheetBean);
        }
    }
    public void ExportToDatabase(Context context)
    {
        for(BridgeResultSheetBean sheetBean:tables)
        {
            BridgeResultSheet resultSheet=new BridgeResultSheet(context,UUID.fromString(sheetBean.getTableId()),sheetBean.getTableInfo());
            for(BridgeResultSheetBean.BridgeContractBean contractBean: sheetBean.getContracts())
            {
                BridgeContract c=new BridgeContract(contractBean.getBrdNum());
                c.setContract(contractBean.getContract());
                resultSheet.addContract(c);
            }
            resultSheet.mTableInfo.setTotalHands(sheetBean.getContracts().size());
            resultSheet.updateTableInfo(resultSheet.mTableInfo);
        }
    }

    public class BridgeResultSheetBean {
        //注意变量名与字段名一致
        private String tableId;
        private List<BridgeContractBean> contracts;
        private BridgeTableInfo tableInfo;
        public BridgeResultSheet ExportToDatabase(Context context)
        {
            BridgeResultSheet resultSheet=new BridgeResultSheet(context,UUID.fromString(tableId),tableInfo);
            for(BridgeResultSheetBean.BridgeContractBean contractBean: contracts)
            {
                BridgeContract c=new BridgeContract(contractBean.getBrdNum());
                c.setContract(contractBean.getContract());
                resultSheet.addContract(c);
            }
            resultSheet.mTableInfo.setTotalHands(contracts.size());
            resultSheet.updateTableInfo(resultSheet.mTableInfo);
            return resultSheet;
        }
        public class BridgeContractBean{
            private int brdNum ;
            private String contract;
            public int getBrdNum() {
                return brdNum;
            }
            public void setBrdNum(int brdNum) {
                this.brdNum = brdNum;
            }
            public String getContract() {
                return contract;
            }
            public void setContract(String contract) {
                this.contract = contract;
            }
        }
        public String getTableId() {
            return tableId;
        }
        public void setTableId(String tableId) {
            this.tableId = tableId;
        }
        public List<BridgeContractBean> getContracts() {
            return contracts;
        }
        public void setContracts(List<BridgeContractBean> contracts) {
            this.contracts = contracts;
        }
        public BridgeTableInfo getTableInfo() {
            return tableInfo;
        }
        public void setTableInfo(BridgeTableInfo tableInfo) {
            this.tableInfo = tableInfo;
        }
    }
    public List<BridgeResultSheetBean> getTables() {
        return tables;
    }
    public void setTables(List<BridgeResultSheetBean> tables) {
        this.tables = tables;
    }
}
