var s2 = ee.ImageCollection("COPERNICUS/S2_SR"),
limits = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/limites");

var userIdentifier = null;
var selectedFC = null;
var userDataList = ee.List([]);
var shpPolygons = null;
var shpRecintos = null;
var selectedRecintos = ee.FeatureCollection([]);
var indexChosen = null;
var fincasSelectedCheckBox = null;
var selectedRecinto = null;
var endDate = ee.Date(Date.now());
var startDate = endDate.advance(-3, "month");
var cloudPercentage = 10;
var selectedGeometry = null;
var fincasLayer = null; 
var label = null;
var selectedRecintosLayer = null;
var controlPanel = null;
var compositeLayer = null;
var downloadURLlabel = null;
var downloadButton = null;
var confirmLocationSelectionButton = null;
var featureCollectionColumn = null

var usedIndexes = {
  'NDVI': false,
  'GNDVI': false,
  'NDMI': false,
  'NBRI': false,
  'NDWI': false,
  'NDSI': false,
  'NDGI': false,
  'B2' : false,
  'B3' : false,
  'B4' : false,
  'B5' : false,
  'B6' : false,
  'B7' : false,
  'B8' : false,
  'B8A' : false,
  'B11' : false,
  'B12' : false,
}
var dictionaryInitialValuesLocation = null;

ui.root.clear();

var mainPanel = ui.Panel({
  layout: ui.Panel.Layout.flow('vertical'),
  style: {width: '360px'}
});

var mapPanel = ui.Panel({layout: ui.Panel.Layout.flow('horizontal'), style: {stretch: 'both'}});

var chartPanel = ui.Panel({
  layout: ui.Panel.Layout.flow('vertical'),
  style: {width: '500px'}
});

ui.root.add(mainPanel);
ui.root.add(mapPanel);

var checkBoxPanel = ui.Panel({
  layout: ui.Panel.Layout.flow('vertical'),
  style: {width: '345px',
  height: '200px'
  }
});

var sentinel2 = s2
    .filterDate('2022-1-1', '2022-2-1')
    .median()

var visParamsS2 = {
    max: 4000, 
    min: 0.0, 
    gamma: 1.0,
    bands: ['B4','B3','B2']
}

var sentinel5P = ee.ImageCollection('COPERNICUS/S5P/NRTI/L3_NO2')
    .select('tropospheric_NO2_column_number_density')
    .filterDate('2022-1-1', '2022-1-10')
    .median()

var visParamsS5P = {
    min: 0,
    max: 0.0003,
    palette: '#000004, #2C105C, 711F81, #B63679, EE605E, #FDAE78, FCFDBF'
}

shpPolygons = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/andalucia_nv3");
shpRecintos = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/andalucia_nv5");

var mapCordoba = ui.Map()
mapPanel.add(mapCordoba);
mapCordoba.setCenter(-4.4,36.7,7);

function userIdentification(userId){
  if(userId === ""){
    return;
  }
  userIdentifier = userId
  var userData = ee.data.listAssets("projects/<PROJECT-ID>/assets/" + userId + "/");
  if(userData.assets!==null){
    userDataList = userData.assets.map(function (object){
      return object.name.split("/").pop();
    });
    userDataList = ee.List(userDataList);
  }
    userDataList = userDataList.getInfo();

  var initialValuesCheckBox = ee.List.repeat(false, ee.List(userDataList).length());
  dictionaryInitialValuesLocation = ee.Dictionary.fromLists(userDataList, initialValuesCheckBox);
  dictionaryInitialValuesLocation = dictionaryInitialValuesLocation.getInfo();
  
  for (var key in dictionaryInitialValuesLocation) {
    checkBoxPanel.add(ui.Checkbox({
      label: key,
      value: dictionaryInitialValuesLocation[key],
      onChange: updateSelectedLocation,
      style: {width: '320px'}
    }))
  }
  
  function updateSelectedLocation(checked, checkbox){
    dictionaryInitialValuesLocation[checkbox.getLabel()]=checked;
    
  }

  userCodeTextbox.setDisabled(true);
  
  mainPanel.insert(1,checkBoxPanel);
 
  confirmLocationSelectionButton  = ui.Button(
  {
    label: 'Confirmar selección', 
    onClick: selectSelectedLocation, 
    style: {width: '320px'}
  });

  mainPanel.insert(2, confirmLocationSelectionButton);
  
  
  var columnTextbox = ui.Textbox({
    placeholder: 'Añade columna usada para agrupar las fincas',
    onChange: function setColumn(value){
              featureCollectionColumn = value;
              },
    style: {width: '320px'}
  });
  
  mainPanel.insert(3, columnTextbox)
}


function selectSelectedLocation(){
  
  mapCordoba.clear()
  ui.root.remove(chartPanel);
  chartPanel.clear();
  mapCordoba.addLayer(sentinel5P, visParamsS5P, 'Sentinel 5P NO2 Enero', false)
  mapCordoba.addLayer(sentinel2, visParamsS2, 'Sentinel 2 Enero', false)

  var addLocation = ee.List([])
  for (var indx in dictionaryInitialValuesLocation) {
    if (dictionaryInitialValuesLocation[indx]){addLocation = addLocation.add(indx);}
  }
  
  fincasSelectedCheckBox = addLocation.getInfo().map(function(location){
    return ee.FeatureCollection("projects/<PROJECT-ID>/assets/" + userIdentifier + "/" + location);
  })
  
  var FeatureCollectionSelectedCheckBox = ee.FeatureCollection(fincasSelectedCheckBox.map(function(finca){
    return ee.Feature(finca.geometry())
  }));
  
  for(var i = 0; i<FeatureCollectionSelectedCheckBox.size().getInfo(); i++){
    selectedRecintos = selectedRecintos.merge(fincasSelectedCheckBox[i])
  }
  
  if (fincasLayer !== null){
      mapCordoba.remove(fincasLayer);
  }
  if ((selectedRecintosLayer !== null)){
    mapCordoba.remove(selectedRecintosLayer);
  }
  
  selectedRecintosLayer = mapCordoba.addLayer({eeObject: FeatureCollectionSelectedCheckBox, name: "Recintos seleccionados"});
  mapCordoba.centerObject(FeatureCollectionSelectedCheckBox);
  selectedGeometry = selectedRecintos.geometry();
  syntheticUseCaseButton.setDisabled(false);
}


var userCodeTextbox = ui.Textbox(
  {
    placeholder: 'Añade código de usuario',
    onChange: userIdentification,
    style: {width: '320px'}
  }
  )
  

mainPanel.add(userCodeTextbox);


var startDateSlider = ui.DateSlider(
  {
    start: ee.Date(s2.first().get('system:time_start')),
    end: ee.Date(Date.now()),
    value:startDate,
    onChange: function setDates(range){
              startDate = range.start();
              },
    period: 1,
    style: {width: '320px'}
  });

var endDateSlider = ui.DateSlider(
  {
    start: ee.Date(s2.first().get('system:time_start')),
    end: ee.Date(Date.now()),
    value: endDate,
    onChange: function setDates(range){
              endDate = range.start();
              },
    period: 1,
    style: {width: '320px'}
  });

var slider = ui.Slider(
  {
    min: 0,
    max: 100,
    step: 1,
    value:10,
    onChange: function setCloudPercentage(value){
              cloudPercentage = value;
              },
    style: {width: '320px'}
  });

mainPanel.add(ui.Label('Filtro fecha de inicio:'));
mainPanel.add(startDateSlider);
mainPanel.add(ui.Label('Filtro fecha final:'));
mainPanel.add(endDateSlider);
mainPanel.add(ui.Label('Filtro porcentaje de nubes:'));
mainPanel.add(slider);

function cloudMaskSentinel(image) {
  var cloudBand = image.select('QA60');
  var cloudBit = 1 << 10;
  var cirrusBit = 1 << 11;
  var cloudMask = cloudBand.bitwiseAnd(cloudBit).eq(0)
    .and(cloudBand.bitwiseAnd(cirrusBit).eq(0));
  return image.updateMask(cloudMask);
}

function syntheticUseCase(){
  mapCordoba.remove(selectedRecintosLayer);
  
  var s2Images = s2.filterDate (startDate, endDate)
    .filterBounds (selectedRecintos) 
    .filterMetadata ('CLOUDY_PIXEL_PERCENTAGE', 'Less_Than', cloudPercentage)
    .map(cloudMaskSentinel);
  var composite = ee.Image(s2Images.median()).clip(selectedRecintos);
  
  if ((compositeLayer !== null)){
      mapCordoba.remove(compositeLayer); 
  }
  compositeLayer = mapCordoba.addLayer({eeObject:composite, visParams:{
    max: 4000, 
    min: 0.0, 
    gamma: 1.0,
    bands: ['B4','B3','B2']}, 
    name: 'TCI composite filtros añadidos Sentinel 2'});
    
  var withndvi = s2Images.map(function(SentinelClip){
    var indexesAdded = []; 
    if (usedIndexes.NDVI){var ndvi = SentinelClip.normalizedDifference(['B8','B4']).rename('NDVI'); indexesAdded.push(ndvi);}
    if (usedIndexes.GNDVI){var gndvi = SentinelClip.normalizedDifference(['B8','B3']).rename('GNDVI'); indexesAdded.push(gndvi);}
    if (usedIndexes.NDMI){var ndmi = SentinelClip.normalizedDifference (['B8', 'B11']).rename('NDMI'); indexesAdded.push(ndmi);}
    if (usedIndexes.NBRI){var nbri = SentinelClip.normalizedDifference(['B8', 'B12']).rename('NBRI'); indexesAdded.push(nbri);}
    if (usedIndexes.NDWI){var ndwi = SentinelClip.normalizedDifference(['B3', 'B8']).rename('NDWI'); indexesAdded.push(ndwi);}
    if (usedIndexes.NDSI){var ndsi = SentinelClip.normalizedDifference(['B3', 'B11']).rename('NDSI'); indexesAdded.push(ndsi);}
    if (usedIndexes.NDGI){var ndgi = SentinelClip.normalizedDifference(['B3', 'B4']).rename('NDGI'); indexesAdded.push(ndgi);}
    return SentinelClip.addBands(indexesAdded);
  });
  
  var addBands = []
  for (var index in usedIndexes) {
    if (usedIndexes[index]){addBands.push(index);}
  }
  
  ui.root.remove(chartPanel);
  chartPanel.clear();
  ui.root.add(chartPanel);
  
  var image = composite
  if (downloadURLlabel !== null){mapCordoba.remove(downloadURLlabel);}
  if (downloadButton !== null){mapCordoba.remove(downloadButton);}
  downloadButton = ui.Button({
  label: "Descargar TIF", 
  onClick: function(){
    var url = image.getDownloadURL({
      name: "TCI",
      bands: ['B4','B3','B2'],
      region: selectedGeometry,
      scale: 50,
      filePerBand: false
    })
    if (downloadURLlabel !== null){mapCordoba.remove(downloadURLlabel);}
    downloadURLlabel = ui.Label({value:url, style: {position:"bottom-left"}});
    mapCordoba.add(downloadURLlabel);
  }, 
      style: {position:"bottom-left"}
  });
  
  mapCordoba.add(downloadButton);
  
  function drawChart(bands){
    var chart = ui.Chart.image.seriesByRegion({
      imageCollection:withndvi,
      regions: selectedRecintos, 
      band: bands, 
      reducer: ee.Reducer.median(),
      scale: 100, 
      seriesProperty:featureCollectionColumn
      });
      
    chart.style().set({
      position: 'bottom-right',
      width: '475px',
      height: '250px'
    });
    
    chartPanel.add(chart);
    
    chart.onClick(function(xValue, yValue, seriesName) {
      if (!xValue) return; 
      var equalDate = ee.Filter.equals('system:time_start', xValue);
      image = ee.Image(withndvi.filter(equalDate).first()).clip(selectedRecintos);
      var savedBands = [bands];
      var savedName = bands;
      if (indexChosen !== null) mapCordoba.remove(indexChosen);
      if (selectedRecinto !== null) mapCordoba.remove(selectedRecinto);
      var recintoChart = selectedRecintos.filterMetadata(featureCollectionColumn, 'Equals', seriesName);
      selectedRecinto = ui.Map.Layer({eeObject: recintoChart, name:'Selected Recinto', opacity: 0.3})
      
      var indexFirstLetterBand = ee.String(savedName).rindex('B')
      if (indexFirstLetterBand.getInfo() === 0){
        indexChosen=ui.Map.Layer(image, {
          max: 10000, 
          min: 0, 
          bands: savedBands}, 
          savedName); 
      }
      else{
        indexChosen=ui.Map.Layer(image, {
          max: 1, 
          min: -1, 
          bands: savedBands}, 
          savedName); 
        
      }
      mapCordoba.add(indexChosen);
      mapCordoba.add(selectedRecinto);
      
      if (label !== null){mapCordoba.remove(label);}
      label = ui.Label((new Date(xValue)).toUTCString());
      mapCordoba.add(label);
      if (downloadURLlabel !== null){mapCordoba.remove(downloadURLlabel);}
      if (downloadButton !== null){mapCordoba.remove(downloadButton);}
      downloadButton = ui.Button({
        label: 'Descargar TIF', 
        onClick: function(){
          var url = image.getDownloadURL({
            name: savedName,
            bands: savedBands,
            region: selectedGeometry,
            scale: 50,
            filePerBand: false
          })
          if (downloadURLlabel !== null){mapCordoba.remove(downloadURLlabel);}
          downloadURLlabel = ui.Label({value:url, style: {position:'bottom-left'}});
          mapCordoba.add(downloadURLlabel);
        }, 
        style: {position:'bottom-left'}
      });
      mapCordoba.add(downloadButton);
    });
  }
  
  var numBands= ee.List(addBands).length().getInfo();
  
  for(var ele = 0; ele<numBands; ele++) { 
    var selectedBand = ee.List(addBands).getString(ele).getInfo();
    drawChart(selectedBand);
    }
}

mainPanel.add(ui.Label('Índices sintéticos, gráfica interactiva'));

function updateUsedIndexes(checked, checkbox){usedIndexes[checkbox.getLabel()]=checked}

for (var index in usedIndexes) {
  mainPanel.add(ui.Checkbox({
    label: index,
    value: usedIndexes[index],
    onChange: updateUsedIndexes,
    style: {width: '320px'}
  }))
}

var syntheticUseCaseButton  = ui.Button(
  {
    label: 'Ejecutar', 
    onClick: syntheticUseCase, 
    disabled: true,
    style: {width: '320px'}
  });

mainPanel.add(syntheticUseCaseButton);