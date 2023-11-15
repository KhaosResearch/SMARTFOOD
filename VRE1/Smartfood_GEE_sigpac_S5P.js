var limits = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/limites"),
    s5P = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_O3_TCL"),
    sitios = ee.FeatureCollection("projects/<PROJECT-ID>/assets/andalucia/sitios");

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
    var s5 = null;
    var dictionaryInitialValuesLocation = null;
    var endDate = ee.Date(Date.now());
    var startDate = endDate.advance(-1, "week");
    
    
    
    
    var usedIndexes = {
      'CH4_column_volume_mixing_ratio_dry_air_bias_corrected': false,
      'CO_column_number_density': false,
      'H2O_column_number_density': false,
      'tropospheric_HCHO_column_number_density': false,
      'NO2_column_number_density': false,
      'tropospheric_NO2_column_number_density': false,
      'stratospheric_NO2_column_number_density': false,
      'O3_column_number_density': false,
      'ozone_tropospheric_vertical_column': false,
      'SO2_column_number_density': false
    }
    
    
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
    
    
    var S5P_CO  = ee.ImageCollection('COPERNICUS/S5P/OFFL/L3_CO')
      .select('CO_column_number_density')
      .filterDate('2023-01-01', '2023-01-30').median();
    
    var band_viz = {
      min: 0,
      max: 0.05,
      palette: ['black', 'blue', 'purple', 'cyan', 'green', 'yellow', 'red']
    };
    
    
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
      mapCordoba.addLayer(S5P_CO, band_viz, 'Sentinel 5sP CO Enero', false)
      mapCordoba.addLayer(sitios, {}, 'Sitios Andalucia', false)
    
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
        start: ee.Date(s5P.first().get('system:time_start')),
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
        start: ee.Date(s5P.first().get('system:time_start')),
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
      
      
      
      
      function returnsGas(value) {
        if (value === 'CH4_column_volume_mixing_ratio_dry_air_bias_corrected') {
          return "CH4"
        }
        if (value === 'CO_column_number_density' || value === 'H2O_column_number_density' ) {
          return "CO"
        }
          if (value === 'tropospheric_HCHO_column_number_density' ) {
          return "HCHO"
        }
          if (value === 'NO2_column_number_density' || value === 'tropospheric_NO2_column_number_density' || value === 'stratospheric_NO2_column_number_density' ) {
          return "NO2"
        }
          if (value === 'O3_column_number_density') {
          return "O3"
        }
          if (value === 'ozone_tropospheric_vertical_column' ) {
          return "O3 TCL"
        }
          if (value === 'SO2_column_number_density' ) {
          return "SO2"
        }
      }
                      
      var addBands = []
      for (var index in usedIndexes) {
        if (usedIndexes[index]){addBands.push(index);}
      }
      ui.root.remove(chartPanel);
      chartPanel.clear();
      ui.root.add(chartPanel);
      
      
      function drawChart(bands, s){
        var s5Images = s.filterDate(startDate, endDate)
          .filterBounds(selectedRecintos.geometry())
          
        var image = null;
        var chart = ui.Chart.image.seriesByRegion({
          imageCollection:s5Images,
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
          image = ee.Image(s5Images.filter(equalDate).first());
          var savedBands = [bands];
          var savedName = bands;
          if (indexChosen !== null) mapCordoba.remove(indexChosen);
          if (selectedRecinto !== null) mapCordoba.remove(selectedRecinto);
          var recintoChart = selectedRecintos.filterMetadata('ID_RECINTO', 'Equals', ee.Number.parse(seriesName));
          selectedRecinto = ui.Map.Layer({eeObject: recintoChart, name:'Selected Recinto', opacity: 0.3})
          
          var stats = image.reduceRegion({
              reducer: ee.Reducer.minMax(),
              geometry: selectedRecintos,
              scale: 100 
            });
    
          var minValue = stats.getNumber(savedName + '_min').getInfo();
          var maxValue = stats.getNumber(savedName + '_max').getInfo();
      
    
          indexChosen=ui.Map.Layer(image, {
              max: maxValue, 
              min: minValue, 
              bands: savedBands}, 
              savedName); 
            
      
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
        var gas = returnsGas(selectedBand);
        if (gas == "CH4"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_CH4")}
        if (gas == "O3"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_O3")}
        if (gas == "NO2"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_NO2")}
        if (gas == "HCHO"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_HCHO")}
        if (gas == "CO"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_CO")}
        if (gas == "SO2"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_SO2")}
        if (gas == "O3 TCL"){s5 = ee.ImageCollection("COPERNICUS/S5P/OFFL/L3_O3_TCL") }
        drawChart(selectedBand, s5);
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