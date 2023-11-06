var s1 = ee.ImageCollection("COPERNICUS/S1_GRD"),
    limits = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/limites");

var userIdentifier = null;
var selectedFC = null;
var userDataList = ee.List([]);
var shpPolygons = null;
var shpRecintos = null;
var selectedRecintos = null;
var indexChosen = null;
var geometrySigPac = null;
var fincasSelectedCheckBox = null;
var recintosIntersectionSigPac = null;
var selectedRecinto = null;
var endDate = ee.Date(Date.now());
var startDate = endDate.advance(-3, "month");
var selectedGeometry = null;
var fincasLayer = null; 
var label = null;
var fincasSigPacLayer = null; 
var fincasSigPac = null;
var selectedRecintosLayer = null;
var controlPanel = null;
var compositeLayer = null;
var downloadURLlabel = null;
var downloadButton = null;
var confirmLocationSelectionButton = null;

var usedIndexes = {
  'VV': false,
  'VH': false,
  'PR': false,
  'RVI_VV': false,
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

var s1_january = s1.filterDate('2022-1-1', '2022-2-1')
    .filter(ee.Filter.listContains('transmitterReceiverPolarisation', 'VH'))
    .filter(ee.Filter.listContains('transmitterReceiverPolarisation', 'VV'))
    .filter(ee.Filter.eq('instrumentMode', 'IW')).median()

    
var rgb_january = ee.Image.rgb(s1_january.select('VV'),
              s1_january.select('VH'),
              s1_january.select('VV').divide(s1_january.select('VH')));

var visParamsS1 = {
    min: [-20, -25, 1],
    max: [0, -5, 15], 
    bands: ['vis-red','vis-green','vis-blue'],
} 

shpPolygons = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/andalucia_nv3");
shpRecintos = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/andalucia_nv5");

var mapCordoba = ui.Map()
mapCordoba.centerObject(limits)
mapPanel.add(mapCordoba);

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

}

function selectSelectedLocation(){
  
  mapCordoba.clear()
  ui.root.remove(chartPanel);
  chartPanel.clear();
  mapCordoba.addLayer(limits, {}, 'Limites SigPac', false)
  mapCordoba.addLayer(rgb_january, visParamsS1, 'Sentinel 1 Enero', false)

  var addLocation = ee.List([])
  for (var indx in dictionaryInitialValuesLocation) {
    if (dictionaryInitialValuesLocation[indx]){addLocation = addLocation.add(indx);}
  }
  
  if (addLocation.size().getInfo() === 0) {
    mapCordoba.centerObject(limits);
    selectRecintoButton.setDisabled(false);
    if ((fincasSigPacLayer !== null) &&  (fincasLayer !== null)){
      mapCordoba.remove(fincasLayer);
      mapCordoba.remove(fincasSigPacLayer);
    }
    if ((selectedRecintosLayer !== null)){
      mapCordoba.remove(selectedRecintosLayer);
    }
    fincasSigPacLayer = mapCordoba.addLayer({eeObject: limits, name: "Área de selección de recinto", opacity: 0.3});
    fincasLayer =  mapCordoba.addLayer({eeObject: limits, name:'', opacity: 0, shown: false});
    return;
  }
  
  fincasSelectedCheckBox = addLocation.getInfo().map(function(location){
    return ee.FeatureCollection("projects/<PROJECT-ID>/assets/" + userIdentifier + "/" + location);
  })
  
  var FeatureCollectionSelectedCheckBox = ee.FeatureCollection(fincasSelectedCheckBox.map(function(finca){
    return ee.Feature(finca.geometry())
  }));

  var fincaIntersectionPolygons =  shpPolygons.filterBounds(FeatureCollectionSelectedCheckBox.geometry().dissolve())
  var uniqueFeaturesCombination =  fincaIntersectionPolygons.distinct(['CD_MUN', 'CD_POL', 'CD_PROV'])
  var polCode = uniqueFeaturesCombination.aggregate_array('CD_POL');
  var municipioCode = uniqueFeaturesCombination.aggregate_array('CD_MUN');
  var provinciaCode = uniqueFeaturesCombination.aggregate_array('CD_PROV');

  var listFincasSigPac = [];
  for (var code = 0; code < polCode.length().getInfo(); code++) {
      var shpFilter = shpRecintos.filter(ee.Filter.and( ee.Filter.eq('CD_MUN',municipioCode.get(code)), ee.Filter.eq('CD_POL', polCode.get(code)), ee.Filter.eq('CD_PROV', provinciaCode.get(code))));
      listFincasSigPac.push(shpFilter);
  }
  
  fincasSigPac = ee.FeatureCollection(listFincasSigPac.map(function(finca){
    return ee.Feature(finca.geometry())
  }));

  
  if ((fincasSigPacLayer !== null) &&  (fincasLayer !== null)){
      mapCordoba.remove(fincasLayer);
      mapCordoba.remove(fincasSigPacLayer);
      
  }
  if ((selectedRecintosLayer !== null)){
    mapCordoba.remove(selectedRecintosLayer);
  }
  fincasSigPacLayer = mapCordoba.addLayer({eeObject: fincasSigPac, name: "Fincas en SigPac", opacity: 0.3});
  fincasLayer =  mapCordoba.addLayer({eeObject: FeatureCollectionSelectedCheckBox, name:'Fincas subidas'});
  mapCordoba.centerObject(FeatureCollectionSelectedCheckBox);
  selectRecintoButton.setDisabled(false);

}

function drawRecinto(){
  
    var drawingTools = mapCordoba.drawingTools();

    drawingTools.setShown(false);
    while (drawingTools.layers().length() > 0) {
      var layer = drawingTools.layers().get(0);
      drawingTools.layers().remove(layer);
    }
    var dummyGeometry =
        ui.Map.GeometryLayer({geometries: null, name: 'geometry', color: '23cba7'});
    
    drawingTools.layers().add(dummyGeometry);
    
    function drawPoint() {
      drawingTools.setShape('point');
      drawingTools.draw();
      drawingTools.onDraw(function disableShowButton(point){
        if (point.intersects(limits).getInfo()){
          showSelectedRecintosButton.setDisabled(false);
        }
      });
    }
    function showSelectedRecintos() {
      getSelectedRecintos()
      mapCordoba.remove(controlPanel);
      mapCordoba.remove(fincasLayer);
      mapCordoba.remove(fincasSigPacLayer);
      if ((selectedRecintosLayer !== null)){
        mapCordoba.remove(selectedRecintosLayer);
      }
      selectedRecintosLayer = mapCordoba.addLayer({eeObject: selectedRecintos, name: "Recintos seleccionados"});
      mapCordoba.centerObject(selectedRecintos)
      while (drawingTools.layers().length() > 0) {
        var layer = drawingTools.layers().get(0);
        drawingTools.layers().remove(layer);
      }
      syntheticUseCaseButton.setDisabled(false)
      confirmLocationSelectionButton.setDisabled(false)
    }
    
    function getSelectedRecintos() {
  
      var drawnGeometries = drawingTools.toFeatureCollection();
      
      drawingTools.setShape(null);
      
      function findIntersection(feature){
          var intersectFilter = ee.Filter.bounds(feature.geometry());
          var b = shpRecintos.filter(intersectFilter).first();
          return b;}
      
      selectedRecintos = drawnGeometries.map(findIntersection, true).distinct(".geo");
      selectedGeometry= selectedRecintos.geometry();

    }
    
    var symbol = {
      polygon: '🔺',
      point: '📍',
      locate: '👁'
    };
    
    var showSelectedRecintosButton = ui.Button({
          label: symbol.locate + ' Confirmar la selección',
          onClick: showSelectedRecintos,
          style: {stretch: 'horizontal'},
          disabled: true
        })
    
    controlPanel = ui.Panel({
      widgets: [
        ui.Label('★ Seleccione los puntos de interés.'),
        ui.Button({
          label: symbol.point + ' Punto',
          onClick: drawPoint,
          style: {stretch: 'horizontal'}
        }),
        showSelectedRecintosButton
        ],
      style: {position: 'bottom-left'},
      layout: null,
    });
  
    mapCordoba.add(controlPanel);
    
    selectRecintoButton.setDisabled(true);
    confirmLocationSelectionButton.setDisabled(true);
}

var userCodeTextbox = ui.Textbox(
  {
    placeholder: 'Añade código de usuario',
    onChange: userIdentification,
    style: {width: '320px'}
  }
  )
  
var selectRecintoButton  = ui.Button(
  {
    label: 'Seleccione recinto', 
    onClick: drawRecinto, 
    disabled: true,
    style: {width: '320px'}
  });

mainPanel.add(userCodeTextbox);

mainPanel.add(selectRecintoButton);

var startDateSlider = ui.DateSlider(
  {
    start: ee.Date(s1.first().get('system:time_start')),
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
    start: ee.Date(s1.first().get('system:time_start')),
    end: ee.Date(Date.now()),
    value: endDate,
    onChange: function setDates(range){
              endDate = range.start();
              },
    period: 1,
    style: {width: '320px'}
  });


mainPanel.add(ui.Label('Filtro fecha de inicio:'));
mainPanel.add(startDateSlider);
mainPanel.add(ui.Label('Filtro fecha final:'));
mainPanel.add(endDateSlider);



function syntheticUseCase(){
  mapCordoba.remove(selectedRecintosLayer);
  

  var s1Images = s1.filterDate(startDate, endDate)
    .filter(ee.Filter.listContains('transmitterReceiverPolarisation', 'VH'))
    .filter(ee.Filter.listContains('transmitterReceiverPolarisation', 'VV'))
    .filterBounds (selectedRecintos) 
    .filter(ee.Filter.eq('instrumentMode', 'IW'))

    
  var composite = ee.Image(s1Images.median()).clip(selectedRecintos);
  
  var rgb = ee.Image.rgb(composite.select('VV'),
                   composite.select('VH'),
                   composite.select('VV').divide(composite.select('VH')));
  
  
  
  if ((compositeLayer !== null)){
      mapCordoba.remove(compositeLayer); 
  }
  compositeLayer = mapCordoba.addLayer({eeObject:rgb, visParams:{bands:['vis-red','vis-green','vis-blue'],min: [-20, -25, 1],max: [0, -5, 15]}, 
    name: 'False RGB'});
    
  var withndvi = s1Images.map(function(SentinelClip){
    var indexesAdded = []; 
    if (usedIndexes.PR) {var pr = SentinelClip.expression('VV / VH',{
      'VV': SentinelClip.select('VV'),
      'VH': SentinelClip.select('VH')
      }
      ).rename('PR');indexesAdded.push(pr);}
    if (usedIndexes.RVI_VV){var rvivv = SentinelClip.expression('(4 * VH) / (VV + VH)',{
      'VV': SentinelClip.select('VV'),
      'VH': SentinelClip.select('VH')
      }
    ).rename('RVI_VV'); indexesAdded.push(rvivv);}

    return SentinelClip.addBands(indexesAdded);
  });
  
  var addBands = []
  for (var index in usedIndexes) {
    if (usedIndexes[index]){addBands.push(index);}
  }
  
  ui.root.remove(chartPanel);
  chartPanel.clear();
  ui.root.add(chartPanel);
  
  var image = rgb
  if (downloadURLlabel !== null){mapCordoba.remove(downloadURLlabel);}
  if (downloadButton !== null){mapCordoba.remove(downloadButton);}
  downloadButton = ui.Button({
  label: "Descargar TIF", 
  onClick: function(){
    var url = image.getDownloadURL({
      name: "RGB",
      bands: ['vis-red','vis-green','vis-blue'],
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
      scale: 30, 
      seriesProperty:'ID_RECINTO'
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
      var recintoChart = selectedRecintos.filterMetadata('ID_RECINTO', 'Equals', ee.Number.parse(seriesName));
      selectedRecinto = ui.Map.Layer({eeObject: recintoChart, name:'Selected Recinto', opacity: 0.3})


      if (savedName === "VV" || savedName === "VH"){
        indexChosen=ui.Map.Layer(image, {
        bands: savedBands, 
        max: 0, 
        min: -30}, 
        savedName);
      }
      else if(savedName === "PR"){
        indexChosen=ui.Map.Layer(image, {
          bands: savedBands,
          max: 1, 
          min: 0
        }, 
          savedName); 
      }
      else{
        indexChosen=ui.Map.Layer(image, {
          bands: savedBands,
          max: 5, 
          min: 0
        }, 
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