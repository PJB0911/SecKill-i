package com.gan.service.impl;

import com.gan.dao.ItemDOMapper;
import com.gan.dao.ItemStockDOMapper;
import com.gan.dataobject.ItemDO;
import com.gan.dataobject.ItemStockDO;
import com.gan.error.BizException;
import com.gan.error.EmBizError;
import com.gan.service.ItemService;
import com.gan.service.PromoService;
import com.gan.service.model.ItemModel;
import com.gan.service.model.PromoModel;
import com.gan.validator.ValidationResult;
import com.gan.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ValidatorImpl validator;
    @Autowired
    private ItemDOMapper itemDOMapper;
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    @Autowired
    private PromoService promoService;

    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BizException {
        //校验入参
        ValidationResult result=validator.validate(itemModel);
        if(result.isHasErrors()){
            throw new BizException(EmBizError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }
        //转化ItemModel变成DataObject
        ItemDO itemDO=this.convertItemDOFromItemModel(itemModel);
        //写入数据库
        itemDOMapper.insertSelective(itemDO);
        //得到生成的主键，并将主键一并写入到itemStock表
        itemModel.setId(itemDO.getId());
        ItemStockDO itemStockDO=this.convertItemStockDOFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);
        //返回创建完成的对象
        return this.getItemById(itemModel.getId());
    }

    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> list=itemDOMapper.listItem();
        List<ItemModel> itemModelList= list.stream().map(itemDO -> {
            ItemStockDO itemStockDO=itemStockDOMapper.selectByItemId(itemDO.getId());
            return this.convertModelFromDataObject(itemDO,itemStockDO);
        }).collect(Collectors.toList());
        return itemModelList;
    }

    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO=itemDOMapper.selectByPrimaryKey(id);
        if(itemDO==null)
            return null;
        //操作获得库存数量
        ItemStockDO itemStockDO=itemStockDOMapper.selectByItemId(itemDO.getId());
        //将dataObject转换成Model
        ItemModel itemModel=convertModelFromDataObject(itemDO,itemStockDO);
        //获取商品的活动信息
        PromoModel promoModel= promoService.getPromoByItemId(itemModel.getId());
        if(promoModel!=null&&promoModel.getStatus()!=3){
            itemModel.setPromoModel(promoModel);
        }
        return itemModel;
    }

    @Override
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount) {
        int affectedRow=itemStockDOMapper.decreaseStock(itemId,amount);
        return (affectedRow>0);
    }

    @Override
    @Transactional
    public void increaseSales(Integer itemId, Integer amount) {
        itemDOMapper.increaseSales(itemId,amount);
    }

    /**
     * 将 itemModel 对象转换成 ItemDO
     * @param itemModel itemModel
     * @return ItemDO
     */
    private ItemDO convertItemDOFromItemModel(ItemModel itemModel){
        if(itemModel==null)
            return null;
        ItemDO itemDO=new ItemDO();
        BeanUtils.copyProperties(itemModel,itemDO);
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    /**
     * 将 itemModel 对象转换成 ItemStockDO
     * @param itemModel itemModel
     * @return ItemStockDO
     */
    private ItemStockDO convertItemStockDOFromItemModel(ItemModel itemModel){
        if(itemModel==null)
            return null;
        ItemStockDO itemStockDO=new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }

    /**
     *  将 DataObject 对象转换成 ItemModel
     * @param itemDO itemDO
     * @param itemStockDO itemStockDO
     * @return  ItemModel
     */
    private ItemModel convertModelFromDataObject(ItemDO itemDO,ItemStockDO itemStockDO){
        ItemModel itemModel=new ItemModel();
        BeanUtils.copyProperties(itemDO,itemModel);
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());
        return itemModel;
    }
}
